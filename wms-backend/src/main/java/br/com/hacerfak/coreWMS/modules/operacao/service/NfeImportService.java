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

            // --- CORREÇÃO CRÍTICA: Desligar namespace awareness facilita encontrar tags
            // simples ---
            dbFactory.setNamespaceAware(false);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            // --- 1. PROCESSAR EMITENTE (Salva TODOS os dados) ---
            NodeList emitList = doc.getElementsByTagName("emit");
            if (emitList == null || emitList.getLength() == 0) {
                throw new IllegalArgumentException("Tag <emit> não encontrada no XML.");
            }
            Element emit = (Element) emitList.item(0);
            Parceiro depositante = processarEmitente(emit);

            // --- 2. DADOS DA NOTA ---
            String nNF = getTagValue("nNF", doc.getDocumentElement());
            String chaveAcesso = extractChaveAcesso(doc);

            if (chaveAcesso != null && recebimentoRepository.existsByChaveAcesso(chaveAcesso)) {
                throw new IllegalArgumentException("Nota Fiscal já importada: " + nNF);
            }

            String dataEmissaoStr = getTagValue("dhEmi", doc.getDocumentElement());
            LocalDateTime dataEmissao = null;
            if (dataEmissaoStr != null && !dataEmissaoStr.isEmpty()) {
                try {
                    dataEmissao = LocalDateTime.parse(dataEmissaoStr.substring(0, 19));
                } catch (Exception e) {
                    System.out.println("Erro data: " + e.getMessage());
                }
            }

            Recebimento recebimento = Recebimento.builder()
                    .numNotaFiscal(nNF)
                    .chaveAcesso(chaveAcesso)
                    .fornecedor(depositante.getNome())
                    .parceiro(depositante) // Vínculo Forte
                    .status(StatusRecebimento.AGUARDANDO)
                    .dataEmissao(dataEmissao)
                    .build();

            // --- 3. PROCESSAR ITENS ---
            NodeList listaItens = doc.getElementsByTagName("det");

            for (int i = 0; i < listaItens.getLength(); i++) {
                Element det = (Element) listaItens.item(i);
                Element prod = (Element) det.getElementsByTagName("prod").item(0);

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
        // Extração Robusta de Documento
        String cnpj = getTagValue("CNPJ", emit);
        if (cnpj == null || cnpj.isEmpty()) {
            cnpj = getTagValue("CPF", emit);
        }

        if (cnpj == null)
            throw new IllegalArgumentException("Emitente sem CNPJ/CPF no XML");

        String nome = getTagValue("xNome", emit);
        String fantasia = getTagValue("xFant", emit);
        String ie = getTagValue("IE", emit);
        String crt = getTagValue("CRT", emit);

        // Busca ou Cria (Upsert)
        Optional<Parceiro> existente = parceiroRepository.findByDocumento(cnpj);
        Parceiro parceiro = existente.orElse(new Parceiro());

        // Se for novo, define defaults importantes
        if (parceiro.getId() == null) {
            parceiro.setTipo("AMBOS"); // <--- Padrão solicitado
            parceiro.setAtivo(true);
            parceiro.setRecebimentoCego(false);
        }

        // Atualiza SEMPRE os dados cadastrais
        parceiro.setDocumento(cnpj);
        parceiro.setNome(nome);
        parceiro.setNomeFantasia(fantasia);
        parceiro.setIe(ie);
        parceiro.setCrt(crt);

        // Endereço Completo (Proteção contra null)
        NodeList enderList = emit.getElementsByTagName("enderEmit");
        if (enderList != null && enderList.getLength() > 0) {
            Element ender = (Element) enderList.item(0);

            parceiro.setLogradouro(getTagValue("xLgr", ender));
            parceiro.setNumero(getTagValue("nro", ender));
            parceiro.setBairro(getTagValue("xBairro", ender));
            parceiro.setCidade(getTagValue("xMun", ender));
            parceiro.setUf(getTagValue("UF", ender));
            parceiro.setCep(getTagValue("CEP", ender));

            String fone = getTagValue("fone", ender);
            parceiro.setTelefone(fone);
        }

        return parceiroRepository.save(parceiro);
    }

    private Produto processarProduto(Element prod, Parceiro depositante) {
        String sku = getTagValue("cProd", prod);
        String nome = getTagValue("xProd", prod);
        String ean = getTagValue("cEAN", prod);
        String ncm = getTagValue("NCM", prod);
        String cest = getTagValue("CEST", prod);
        String uCom = getTagValue("uCom", prod);
        String vUnComStr = getTagValue("vUnCom", prod);
        BigDecimal valorUnitario = (vUnComStr != null) ? new BigDecimal(vUnComStr) : BigDecimal.ZERO;

        Optional<Produto> existente = produtoRepository.findBySkuAndDepositante(sku, depositante);
        Produto produto = existente.orElse(new Produto());

        if (produto.getId() == null) {
            produto.setSku(sku);
            produto.setDepositante(depositante);
            produto.setAtivo(true);
        }

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
            if (el.hasAttribute("Id")) {
                return el.getAttribute("Id").replace("NFe", "");
            }
        }
        return null;
    }
}