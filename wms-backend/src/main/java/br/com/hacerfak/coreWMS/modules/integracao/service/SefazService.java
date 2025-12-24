package br.com.hacerfak.coreWMS.modules.integracao.service;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.EmpresaDados;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaDadosRepository;
import br.com.hacerfak.coreWMS.modules.integracao.dto.CnpjResponse;
import br.com.hacerfak.coreWMS.modules.integracao.util.SefazConstants;
import br.com.hacerfak.coreWMS.core.service.CryptoService;
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
import java.util.Scanner;

@Service
@RequiredArgsConstructor
public class SefazService {

    private final EmpresaDadosRepository empresaDadosRepository;
    private final CryptoService cryptoService;

    public CnpjResponse consultarCadastro(String uf, String cnpj, String ie) {
        try {
            // Busca na nova tabela
            EmpresaDados config = empresaDadosRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Dados da empresa não encontrados"));

            if (config.getCertificadoArquivo() == null) {
                throw new IllegalArgumentException("Certificado Digital não configurado.");
            }

            // --- DESCRIPTOGRAFIA ---
            String senhaDecifrada = cryptoService.decrypt(config.getCertificadoSenha());

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(config.getCertificadoArquivo()),
                    senhaDecifrada.toCharArray()); // Usa senha limpa

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, senhaDecifrada.toCharArray()); // Usa senha limpa

            // ... (Restante da lógica SSL igual) ...
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());

            String ufTarget = uf != null ? uf.toUpperCase() : "SP";
            String urlStr = SefazConstants.URLS_SEFAZ.getOrDefault(ufTarget, SefazConstants.URLS_SEFAZ.get("RS")); // Fallback
                                                                                                                   // corrigido
                                                                                                                   // para
                                                                                                                   // não
                                                                                                                   // dar
            // null pointer se map estiver
            // vazio na compilação deste
            // exemplo

            String xmlBody = construirXmlConsulta(ufTarget, cnpj, ie);
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
            dto.setUf(getTagValue(infCad, "UF"));
            dto.setRazaoSocial(getTagValue(infCad, "xNome"));
            dto.setNomeFantasia(getTagValue(infCad, "xFant"));

            String cSit = getTagValue(infCad, "cSit");
            dto.setSituacao(cSit.equals("1") ? "ATIVA" : "INATIVA/BAIXADA");

            dto.setIe(getTagValue(infCad, "IE"));
            dto.setCnaePrincipal(getTagValue(infCad, "CNAE"));

            // --- MAPEAMENTO AVANÇADO DO CRT ---
            // Tenta obter o CRT (código) ou o xReg (texto descritivo)
            // A prioridade é xReg porque nele temos a descrição completa para o mapeamento
            // do CSV
            String crtTexto = getTagValue(infCad, "xRegApur");

            dto.setRegimeTributario(mapRegimeTributario(crtTexto));

            Element ender = (Element) infCad.getElementsByTagNameNS("*", "ender").item(0);
            if (ender != null) {
                dto.setLogradouro(getTagValue(ender, "xLgr"));
                dto.setNumero(getTagValue(ender, "nro"));
                dto.setComplemento(getTagValue(ender, "xCpl"));
                dto.setBairro(getTagValue(ender, "xBairro"));

                String cepRaw = getTagValue(ender, "CEP");
                dto.setCep(cepRaw);

                dto.setCidade(getTagValue(ender, "xMun"));
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
     * Mapeia o texto ou código retornado pela SEFAZ para os códigos internos.
     */
    private String mapRegimeTributario(String crtRaw) {
        if (crtRaw == null || crtRaw.trim().isEmpty()) {
            return "3"; // Padrão: Regime Normal se não vier nada
        }

        String textoNormalizado = crtRaw.trim().toUpperCase();

        // 1. Tenta encontrar no Mapa Exato (vindo do CSV)
        if (SefazConstants.MAPEAMENTO_CRT.containsKey(textoNormalizado)) {
            return SefazConstants.MAPEAMENTO_CRT.get(textoNormalizado);
        }

        // 2. Se não achou exato, tenta correspondência direta numérica (caso venha "1",
        // "2"...)
        if (textoNormalizado.equals("1") || textoNormalizado.equals("2") || textoNormalizado.equals("3")
                || textoNormalizado.equals("4")) {
            return textoNormalizado;
        }

        // 3. Fallback inteligente por palavras-chave (para textos novos não mapeados)
        if (textoNormalizado.contains("MEI") || textoNormalizado.contains("SIMEI"))
            return "4";
        if (textoNormalizado.contains("EXCESSO"))
            return "2"; // Excesso de sublimite
        if (textoNormalizado.contains("SIMPLES"))
            return "1"; // Simples Nacional

        // Se contiver "NORMAL", "LUCRO", "REAL", "PRESUMIDO" -> 3
        if (textoNormalizado.contains("NORMAL") || textoNormalizado.contains("LUCRO")
                || textoNormalizado.contains("REAL") || textoNormalizado.contains("PRESUMIDO")) {
            return "3";
        }

        // Fallback final
        return "3";
    }
}