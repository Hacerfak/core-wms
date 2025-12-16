package br.com.hacerfak.coreWMS.modules.operacao.service;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.operacao.domain.ItemRecebimento;
import br.com.hacerfak.coreWMS.modules.operacao.domain.Recebimento;
import br.com.hacerfak.coreWMS.modules.operacao.domain.StatusRecebimento;
import br.com.hacerfak.coreWMS.modules.operacao.repository.RecebimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NfeImportService {

    private final RecebimentoRepository recebimentoRepository;
    private final ProdutoRepository produtoRepository;
    private final ParceiroRepository parceiroRepository;

    @Transactional
    public Recebimento importarXml(MultipartFile file) {
        try {
            InputStream is = file.getInputStream();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            // --- 1. PROCESSAR EMITENTE (Parceiro Completo) ---
            Element emit = (Element) doc.getElementsByTagName("emit").item(0);
            Parceiro depositante = processarEmitente(emit);

            // --- 2. DADOS DA NOTA ---
            String nNF = getTagValue("nNF", doc.getDocumentElement());
            String chaveAcesso = extractChaveAcesso(doc);

            if (recebimentoRepository.existsByChaveAcesso(chaveAcesso)) {
                throw new IllegalArgumentException("Nota Fiscal já importada: " + nNF);
            }

            // Validação de Duplicidade
            if (chaveAcesso != null && recebimentoRepository.existsByChaveAcesso(chaveAcesso)) {
                // Opcional: throw new IllegalArgumentException("Nota já importada.");
                System.out.println("Aviso: Nota já importada, chave: " + chaveAcesso);
            }

            // LER A DATA DE EMISSÃO
            String dataEmissaoStr = getTagValue("dhEmi", doc.getDocumentElement()); // Formato:
                                                                                    // 2023-10-25T14:30:00-03:00
            LocalDateTime dataEmissao = null;
            if (dataEmissaoStr != null && !dataEmissaoStr.isEmpty()) {
                // Remove o timezone para simplificar e converte para LocalDateTime
                // (Para produção robusta, ideal seria usar OffsetDateTime, mas isso funciona
                // para 99% dos casos locais)
                try {
                    dataEmissao = LocalDateTime.parse(dataEmissaoStr.substring(0, 19));
                } catch (Exception e) {
                    System.out.println("Erro ao converter data emissão: " + e.getMessage());
                }
            }

            Recebimento recebimento = Recebimento.builder()
                    .numNotaFiscal(nNF)
                    .chaveAcesso(chaveAcesso)
                    .fornecedor(depositante.getNome())
                    .status(StatusRecebimento.AGUARDANDO)
                    .dataEmissao(dataEmissao)
                    .build();

            // --- 3. PROCESSAR ITENS (Produtos Completos) ---
            NodeList listaItens = doc.getElementsByTagName("det");

            for (int i = 0; i < listaItens.getLength(); i++) {
                Element det = (Element) listaItens.item(i);
                Element prod = (Element) det.getElementsByTagName("prod").item(0);

                // Cadastra ou Atualiza o Produto com dados completos
                Produto produto = processarProduto(prod, depositante);

                String qCom = getTagValue("qCom", prod);

                ItemRecebimento item = ItemRecebimento.builder()
                        .recebimento(recebimento)
                        .produto(produto)
                        .quantidadeNota(new BigDecimal(qCom))
                        .quantidadeConferida(BigDecimal.ZERO)
                        .build();

                recebimento.getItens().add(item);
            }

            return recebimentoRepository.save(recebimento);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao processar XML: " + e.getMessage());
        }
    }

    private Parceiro processarEmitente(Element emit) {
        String cnpj = getTagValue("CNPJ", emit);
        if (cnpj == null)
            cnpj = getTagValue("CPF", emit);

        String nome = getTagValue("xNome", emit);
        String fantasia = getTagValue("xFant", emit);
        String ie = getTagValue("IE", emit);

        // Endereço (Tag enderEmit)
        Element ender = (Element) emit.getElementsByTagName("enderEmit").item(0);
        String logradouro = getTagValue("xLgr", ender);
        String numero = getTagValue("nro", ender);
        String bairro = getTagValue("xBairro", ender);
        String cidade = getTagValue("xMun", ender);
        String uf = getTagValue("UF", ender);
        String cep = getTagValue("CEP", ender);
        String fone = getTagValue("fone", ender);

        // Busca existente ou cria novo (Pattern: Upsert)
        Optional<Parceiro> existente = parceiroRepository.findByDocumento(cnpj);
        Parceiro parceiro = existente.orElse(new Parceiro());

        // Atualiza os dados (garante que o cadastro esteja sempre fresco com o último
        // XML)
        parceiro.setDocumento(cnpj);
        parceiro.setNome(nome);
        parceiro.setNomeFantasia(fantasia);
        parceiro.setIe(ie);

        // Dados de Endereço
        parceiro.setLogradouro(logradouro);
        parceiro.setNumero(numero);
        parceiro.setBairro(bairro);
        parceiro.setCidade(cidade);
        parceiro.setUf(uf);
        parceiro.setCep(cep);
        parceiro.setTelefone(fone);

        return parceiroRepository.save(parceiro);
    }

    private Produto processarProduto(Element prod, Parceiro depositante) {
        String sku = getTagValue("cProd", prod);
        String nome = getTagValue("xProd", prod);
        String ean = getTagValue("cEAN", prod);
        String ncm = getTagValue("NCM", prod);
        String cest = getTagValue("CEST", prod); // Novo
        String uCom = getTagValue("uCom", prod);

        String vUnComStr = getTagValue("vUnCom", prod); // Valor Unitário
        BigDecimal valorUnitario = (vUnComStr != null) ? new BigDecimal(vUnComStr) : BigDecimal.ZERO;

        Optional<Produto> existente = produtoRepository.findBySkuAndDepositante(sku, depositante);
        Produto produto = existente.orElse(new Produto());

        // Se for novo, seta o SKu e Depositante (que são imutáveis na chave lógica)
        if (produto.getId() == null) {
            produto.setSku(sku);
            produto.setDepositante(depositante);
            produto.setAtivo(true);
        }

        // Atualiza dados cadastrais
        produto.setNome(nome);
        produto.setEan13((ean != null && !ean.equals("SEM GTIN")) ? ean : null);
        produto.setUnidadeMedida(uCom);
        produto.setNcm(ncm);
        produto.setCest(cest);
        produto.setValorUnitarioPadrao(valorUnitario);

        return produtoRepository.save(produto);
    }

    private String getTagValue(String tag, Element element) {
        if (element == null)
            return null;
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private String extractChaveAcesso(Document doc) {
        NodeList infNFe = doc.getElementsByTagName("infNFe");
        if (infNFe.getLength() > 0) {
            Element el = (Element) infNFe.item(0);
            // O ID da NFe fica no atributo "Id" da tag infNFe e tem o prefixo "NFe"
            if (el.hasAttribute("Id")) {
                return el.getAttribute("Id").replace("NFe", "");
            }
        }
        return null;
    }
}