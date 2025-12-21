package br.com.hacerfak.coreWMS.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.MessageDigest;
import java.util.Arrays;

@Service
public class CryptoService {

    @Value("${api.security.crypto.secret:Sup3rS3cr3tK3yPadraoWMS2025!}")
    private String secret;

    private SecretKeySpec getKey() {
        try {
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // Use apenas os primeiros 128 bits
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar chave de criptografia", e);
        }
    }

    public String encrypt(String strToEncrypt) {
        try {
            if (strToEncrypt == null)
                return null;
            SecretKeySpec secretKey = getKey();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar dados: " + e.getMessage(), e);
        }
    }

    public String decrypt(String strToDecrypt) {
        try {
            if (strToDecrypt == null)
                return null;
            SecretKeySpec secretKey = getKey();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao descriptografar dados: " + e.getMessage(), e);
        }
    }
}