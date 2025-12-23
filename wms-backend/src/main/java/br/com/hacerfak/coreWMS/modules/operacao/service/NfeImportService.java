package br.com.hacerfak.coreWMS.modules.operacao.service;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.operacao.domain.ItemSolicitacaoEntrada;
import br.com.hacerfak.coreWMS.modules.operacao.domain.SolicitacaoEntrada;
import br.com.hacerfak.coreWMS.modules.operacao.repository.SolicitacaoEntradaRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NfeImportService {

    private final SolicitacaoEntradaRepository solicitacaoRepository;
    private final RecebimentoWorkflowService inboundWorkflowService; // <--- O novo orquestrador
    private final ProdutoRepository produtoRepository;
    private final ParceiroRepository parceiroRepository;

    @Transactional
    public SolicitacaoEntrada importarXml(MultipartFile file) {
        try {
            InputStream is = file.getInputStream();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(false); // Facilita a leitura das tags

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            // --- 1. PROCESSAR EMITENTE ---
            NodeList emitList = doc.getElementsByTagName("emit");
            if (emitList == null || emitList.getLength() == 0) {
                throw new IllegalArgumentException("Tag <emit> não encontrada no XML.");
            }
            Element emit = (Element) emitList.item(0);
            Parceiro fornecedor = processarEmitente(emit);

            // --- 2. DADOS DA NOTA ---
            String nNF = getTagValue("nNF", doc.getDocumentElement());
            String chaveAcesso = extractChaveAcesso(doc);

            // Validação de Duplicidade na nova estrutura
            if (chaveAcesso != null && solicitacaoRepository.existsByChaveAcesso(chaveAcesso)) {
                throw new IllegalArgumentException("Nota Fiscal já importada: " + nNF);
            }

            // Extração da Data
            String dataEmissaoStr = getTagValue("dhEmi", doc.getDocumentElement());
            LocalDateTime dataEmissao = null;
            if (dataEmissaoStr != null && !dataEmissaoStr.isEmpty()) {
                // Tenta lidar com o formato UTC (ex: 2023-10-01T10:00:00-03:00)
                try {
                    dataEmissao = LocalDateTime.parse(dataEmissaoStr, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    // Fallback simples se der erro no parser
                    try {
                        dataEmissao = LocalDateTime.parse(dataEmissaoStr.substring(0, 19));
                    } catch (Exception ignored) {
                    }
                }
            }

            // Cria a SOLICITAÇÃO (em vez de Recebimento)
            SolicitacaoEntrada solicitacao = SolicitacaoEntrada.builder()
                    .codigoExterno(nNF) // Usamos o número da nota como código externo
                    .numNotaFiscal(nNF)
                    .chaveAcesso(chaveAcesso)
                    .fornecedor(fornecedor)
                    .dataEmissao(dataEmissao)
                    .build();

            // --- 3. PROCESSAR ITENS ---
            NodeList listaItens = doc.getElementsByTagName("det");

            for (int i = 0; i < listaItens.getLength(); i++) {
                Element det = (Element) listaItens.item(i);
                Element prod = (Element) det.getElementsByTagName("prod").item(0);

                Produto produto = processarProduto(prod, fornecedor);
                String qCom = getTagValue("qCom", prod);

                ItemSolicitacaoEntrada item = ItemSolicitacaoEntrada.builder()
                        .solicitacao(solicitacao)
                        .produto(produto)
                        .quantidadePrevista(new BigDecimal(qCom))
                        .quantidadeConferida(BigDecimal.ZERO)
                        .build();

                solicitacao.getItens().add(item);
            }

            // 🔥 AQUI ESTÁ A MUDANÇA PRINCIPAL:
            // Não salvamos direto. Passamos para o Workflow iniciar o processo.
            // O Workflow vai definir o status inicial, criar as tarefas, etc.
            return inboundWorkflowService.iniciarProcessoEntrada(solicitacao);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao processar XML: " + e.getMessage());
        }
    }

    // --- MÉTODOS AUXILIARES (Mantidos praticamente iguais, apenas ajustes de
    // tipos) ---

    private Parceiro processarEmitente(Element emit) {
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

        Optional<Parceiro> existente = parceiroRepository.findByDocumento(cnpj);
        Parceiro parceiro = existente.orElse(new Parceiro());

        if (parceiro.getId() == null) {
            parceiro.setTipo("AMBOS");
            parceiro.setAtivo(true);
            parceiro.setRecebimentoCego(false); // Default
        }

        parceiro.setDocumento(cnpj);
        parceiro.setNome(nome);
        parceiro.setNomeFantasia(fantasia);
        parceiro.setIe(ie);
        parceiro.setCrt(crt);

        // Endereço
        NodeList enderList = emit.getElementsByTagName("enderEmit");
        if (enderList != null && enderList.getLength() > 0) {
            Element ender = (Element) enderList.item(0);
            parceiro.setLogradouro(getTagValue("xLgr", ender));
            parceiro.setNumero(getTagValue("nro", ender));
            parceiro.setBairro(getTagValue("xBairro", ender));
            parceiro.setCidade(getTagValue("xMun", ender));
            parceiro.setUf(getTagValue("UF", ender));
            parceiro.setCep(getTagValue("CEP", ender));
            parceiro.setTelefone(getTagValue("fone", ender));
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