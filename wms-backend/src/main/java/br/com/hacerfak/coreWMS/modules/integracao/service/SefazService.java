package br.com.hacerfak.coreWMS.modules.integracao.service;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.EmpresaConfig;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaConfigRepository;
import br.com.hacerfak.coreWMS.modules.integracao.dto.CnpjResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Service
@RequiredArgsConstructor
public class SefazService {

    private final EmpresaConfigRepository empresaConfigRepository;

    // Mapa de URLs atualizado conforme seu POSTMAN
    private static final Map<String, String> URLS_SEFAZ = new HashMap<>();
    static {
        // SVRS (Atende AC, AL, AP, CE, DF, ES, PA, PB, PI, RJ, RN, RO, RR, RS, SC, SE,
        // TO)
        String svrs = "https://cad.svrs.rs.gov.br/ws/cadconsultacadastro/cadconsultacadastro4.asmx";

        URLS_SEFAZ.put("RS", svrs);
        URLS_SEFAZ.put("SC", svrs);
        URLS_SEFAZ.put("RJ", svrs);
        URLS_SEFAZ.put("ES", svrs);
        URLS_SEFAZ.put("AC", svrs);
        URLS_SEFAZ.put("RN", svrs);
        URLS_SEFAZ.put("PB", svrs);
        URLS_SEFAZ.put("DF", svrs);
        URLS_SEFAZ.put("TO", svrs);
        URLS_SEFAZ.put("CE", svrs);
        URLS_SEFAZ.put("AL", svrs);
        URLS_SEFAZ.put("AP", svrs);
        URLS_SEFAZ.put("PA", svrs);
        URLS_SEFAZ.put("PI", svrs);
        URLS_SEFAZ.put("RO", svrs);
        URLS_SEFAZ.put("RR", svrs);
        URLS_SEFAZ.put("SE", svrs);

        // Específicos
        URLS_SEFAZ.put("SP", "https://nfe.fazenda.sp.gov.br/ws/cadconsultacadastro4.asmx");
        URLS_SEFAZ.put("PR", "https://nfe.sefa.pr.gov.br/nfe/CadConsultaCadastro4");
        URLS_SEFAZ.put("MG", "https://nfe.fazenda.mg.gov.br/nfe2/services/CadConsultaCadastro4");
        URLS_SEFAZ.put("BA", "https://nfe.sefaz.ba.gov.br/webservices/CadConsultaCadastro4/CadConsultaCadastro4.asmx");
        URLS_SEFAZ.put("GO", "https://nfe.sefaz.go.gov.br/nfe/services/CadConsultaCadastro4");
        URLS_SEFAZ.put("PE", "https://nfe.sefaz.pe.gov.br/nfe-service/services/CadConsultaCadastro4");
        URLS_SEFAZ.put("MT", "https://nfe.sefaz.mt.gov.br/nfews/v2/services/CadConsultaCadastro4");
        URLS_SEFAZ.put("MS", "https://nfe.sefaz.ms.gov.br/ws/CadConsultaCadastro4");
        URLS_SEFAZ.put("AM", "https://nfe.sefaz.am.gov.br/services2/services/CadConsultaCadastro4");
    }

    public CnpjResponse consultarCadastro(String uf, String cnpj, String ie) {
        try {
            EmpresaConfig config = empresaConfigRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Configuração da empresa não encontrada"));
            if (config.getCertificadoArquivo() == null) {
                throw new IllegalArgumentException("Certificado Digital não configurado.");
            }

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(config.getCertificadoArquivo()),
                    config.getCertificadoSenha().toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, config.getCertificadoSenha().toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

            String ufTarget = uf != null ? uf.toUpperCase() : "SP";
            String urlStr = URLS_SEFAZ.get(ufTarget);
            if (urlStr == null)
                urlStr = URLS_SEFAZ.get("RS");

            String xmlBody = construirXmlConsulta(ufTarget, cnpj, ie);

            System.out.println("Enviando para SEFAZ (" + urlStr + "): " + xmlBody);

            String responseXml = enviarRequestSoap(urlStr, xmlBody, sslContext, ufTarget);

            return parseResponse(responseXml);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro na comunicação com SEFAZ: " + e.getMessage());
        }
    }

    private String enviarRequestSoap(String urlStr, String xmlBody, SSLContext sslContext, String uf)
            throws IOException {
        URL url = URI.create(urlStr).toURL();

        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(sslContext.getSocketFactory());
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        if ("MT".equals(uf) || "PE".equals(uf)) {
            conn.setRequestProperty("Content-Type", "application/soap+xml;charset=utf-8;action=\"consultaCadastro\"");
        } else {
            conn.setRequestProperty("Content-Type", "application/soap+xml;charset=utf-8");
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(xmlBody.getBytes());
        }

        int responseCode = conn.getResponseCode();
        InputStream streamToRead;

        if (responseCode >= 200 && responseCode < 300) {
            streamToRead = conn.getInputStream();
        } else {
            streamToRead = conn.getErrorStream();
            if (streamToRead == null)
                streamToRead = conn.getInputStream();
        }

        try (Scanner scanner = new Scanner(streamToRead)) {
            scanner.useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";

            if (responseCode >= 300) {
                System.err.println("SEFAZ ERRO HTTP " + responseCode + ": " + response);
                if (response.contains("Fault")) {
                    throw new IOException("SEFAZ retornou erro: " + response);
                }
                throw new IOException("HTTP Error " + responseCode + ": " + response);
            }
            return response;
        }
    }

    private String construirXmlConsulta(String uf, String cnpj, String ie) {
        String cnpjL = cnpj != null ? cnpj.replaceAll("\\D", "") : "";
        String ieL = ie != null ? ie.replaceAll("\\D", "") : "";

        String criterio = (!cnpjL.isEmpty())
                ? "<CNPJ>" + cnpjL + "</CNPJ>"
                : "<IE>" + ieL + "</IE>";

        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">"
                +
                "<soap12:Body>" +
                "<nfeDadosMsg xmlns=\"http://www.portalfiscal.inf.br/nfe/wsdl/CadConsultaCadastro4\">" +
                "<ConsCad xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"2.00\">" +
                "<infCons>" +
                "<xServ>CONS-CAD</xServ>" +
                "<UF>" + uf + "</UF>" +
                criterio +
                "</infCons>" +
                "</ConsCad>" +
                "</nfeDadosMsg>" +
                "</soap12:Body>" +
                "</soap12:Envelope>";
    }

    private CnpjResponse parseResponse(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xml)));

            NodeList cStatList = doc.getElementsByTagNameNS("*", "cStat");
            if (cStatList.getLength() > 0) {
                String cStat = cStatList.item(0).getTextContent();
                if (!cStat.equals("111") && !cStat.equals("112")) {
                    NodeList xMotivoList = doc.getElementsByTagNameNS("*", "xMotivo");
                    String xMotivo = xMotivoList.getLength() > 0 ? xMotivoList.item(0).getTextContent()
                            : "Erro desconhecido";
                    throw new RuntimeException("SEFAZ (" + cStat + "): " + xMotivo);
                }
            }

            NodeList infCadList = doc.getElementsByTagNameNS("*", "infCad");
            if (infCadList.getLength() == 0) {
                throw new RuntimeException("Cadastro não encontrado na base da SEFAZ.");
            }

            Element infCad = (Element) infCadList.item(0);

            CnpjResponse dto = new CnpjResponse();
            dto.setCnpj(getTagValue(infCad, "CNPJ"));
            dto.setRazaoSocial(getTagValue(infCad, "xNome"));
            dto.setNomeFantasia(getTagValue(infCad, "xFant"));

            String cSit = getTagValue(infCad, "cSit");
            dto.setSituacao(cSit.equals("1") ? "ATIVA" : "INATIVA/BAIXADA");

            dto.setIe(getTagValue(infCad, "IE"));
            dto.setCnaePrincipal(getTagValue(infCad, "CNAE"));

            // --- MAPEAMENTO INTELIGENTE DO CRT (Texto -> Código) ---
            // A tag geralmente é CRT ou xReg
            String crtRaw = getTagValue(infCad, "CRT");
            if (crtRaw.isEmpty())
                crtRaw = getTagValue(infCad, "xReg"); // Alguns estados retornam em xReg

            dto.setRegimeTributario(mapRegimeTributario(crtRaw));

            Element ender = (Element) infCad.getElementsByTagNameNS("*", "ender").item(0);
            if (ender != null) {
                dto.setLogradouro(getTagValue(ender, "xLgr"));
                dto.setNumero(getTagValue(ender, "nro"));
                dto.setComplemento(getTagValue(ender, "xCpl"));
                dto.setBairro(getTagValue(ender, "xBairro"));

                String cepRaw = getTagValue(ender, "CEP");
                dto.setCep(cepRaw);

                dto.setCidade(getTagValue(ender, "xMun"));
                dto.setUf(getTagValue(ender, "UF"));
            }

            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            throw new RuntimeException("Erro ao ler XML da SEFAZ: " + e.getMessage());
        }
    }

    private String getTagValue(Element element, String tagName) {
        NodeList list = element.getElementsByTagNameNS("*", tagName);
        if (list != null && list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return "";
    }

    /**
     * Mapeia o texto retornado pela SEFAZ para os códigos internos.
     * 1 = Simples Nacional
     * 2 = Simples Nacional - Excesso
     * 3 = Regime Normal
     * 4 = MEI
     */
    private String mapRegimeTributario(String crtRaw) {
        if (crtRaw == null || crtRaw.trim().isEmpty()) {
            return "3"; // Padrão: Regime Normal se não vier nada
        }

        String texto = crtRaw.trim().toUpperCase();

        // 1. Tenta correspondência direta numérica
        if (texto.equals("1") || texto.equals("2") || texto.equals("3") || texto.equals("4")) {
            return texto;
        }

        // 2. Busca por palavras-chave (Ordem importa!)

        // Excesso de Sublimite (Prioridade sobre "Simples")
        if (texto.contains("EXCESSO")) {
            return "2";
        }

        // MEI
        if (texto.contains("MEI") || texto.contains("MICROEMPREENDEDOR")) {
            return "4";
        }

        // Simples Nacional
        if (texto.contains("SIMPLES")) {
            return "1";
        }

        // Regime Normal
        if (texto.contains("NORMAL") || texto.contains("LUCRO")) {
            return "3";
        }

        // Fallback
        return "3";
    }
}