package br.com.hacerfak.coreWMS.modules.cadastro.service;

import lombok.Getter;
import lombok.Builder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CertificadoService {

    @Getter
    @Builder
    public static class DadosCertificado {
        private String razaoSocial;
        private String cnpj;
        private LocalDate validade;
    }

    public DadosCertificado extrairDados(MultipartFile arquivoPfx, String senha) {
        try {
            // 1. Carrega o KeyStore (Formato PKCS12 é o padrão do .pfx A1)
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream stream = arquivoPfx.getInputStream()) {
                keyStore.load(stream, senha.toCharArray());
            }

            // 2. Pega o "Alias" (o nome interno do certificado)
            Enumeration<String> aliases = keyStore.aliases();
            String alias = null;
            while (aliases.hasMoreElements()) {
                alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    break;
                }
            }

            if (alias == null) {
                throw new RuntimeException("Nenhum certificado válido encontrado no arquivo.");
            }

            // 3. Extrai o X509Certificate
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

            // 4. Parseia o Subject (Onde estão os dados: CN=EMPRESA X, O=ICP-Brasil...)
            String subject = cert.getSubjectX500Principal().getName();

            return DadosCertificado.builder()
                    .razaoSocial(extrairRazaoSocial(subject))
                    .cnpj(extrairCnpj(subject))
                    .validade(cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler certificado: " + e.getMessage());
        }
    }

    // Extrai o nome (Geralmente no CN=NOME:CNPJ ou apenas CN=NOME)
    private String extrairRazaoSocial(String subject) {
        // Padrão comum: CN=RAZAO SOCIAL:00000000000000
        String cn = extrairValor(subject, "CN");
        if (cn.contains(":")) {
            return cn.split(":")[0];
        }
        return cn;
    }

    // Extrai o CNPJ (Geralmente vem colado no nome ou em OID específico)
    // Implementação simplificada buscando sequência de 14 dígitos
    private String extrairCnpj(String subject) {
        // Tenta achar 14 digitos seguidos no CN ou na string inteira
        Pattern pattern = Pattern.compile("\\b\\d{14}\\b");
        Matcher matcher = pattern.matcher(subject);
        if (matcher.find()) {
            return matcher.group();
        }
        // Se falhar, tenta pegar do final do CN (Padrão ICP-Brasil antigo)
        String cn = extrairValor(subject, "CN");
        String[] parts = cn.split(":");
        if (parts.length > 1) {
            String possivelCnpj = parts[parts.length - 1].trim();
            if (possivelCnpj.length() == 14)
                return possivelCnpj;
        }
        throw new RuntimeException("Não foi possível identificar o CNPJ no certificado.");
    }

    private String extrairValor(String text, String key) {
        // Lógica simples de parse de X500 Name (ex: CN=Valor, O=Outro)
        String[] tokens = text.split(",");
        for (String token : tokens) {
            String[] pair = token.trim().split("=");
            if (pair.length == 2 && pair[0].equalsIgnoreCase(key)) {
                return pair[1];
            }
        }
        return "";
    }
}