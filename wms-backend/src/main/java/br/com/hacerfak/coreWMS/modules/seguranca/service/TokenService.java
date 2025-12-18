package br.com.hacerfak.coreWMS.modules.seguranca.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TokenService {

    @Value("${api.security.token.secret:segredo}")
    private String secret;

    public String generateToken(Usuario usuario, String tenantId, List<String> authorities) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var builder = JWT.create()
                    .withIssuer("wms-api")
                    .withSubject(usuario.getLogin())
                    .withExpiresAt(genExpirationDate()); // Usa a nova data segura

            if (tenantId != null) {
                builder.withClaim("tenant", tenantId);
            }

            if (authorities != null && !authorities.isEmpty()) {
                builder.withClaim("roles", authorities);
            }

            return builder.sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    // Sobrecarga para compatibilidade
    public String generateToken(Usuario usuario, String tenantId) {
        return generateToken(usuario, tenantId, List.of());
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("wms-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            // --- LOG DE DIAGNÓSTICO ---
            System.out.println(">>> FALHA NA VALIDAÇÃO DO TOKEN: " + exception.getMessage());
            return "";
        }
    }

    public String getTenantFromToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer("wms-api")
                    .build()
                    .verify(token);

            if (jwt.getClaim("tenant").isNull()) {
                return null;
            }
            return jwt.getClaim("tenant").asString();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    // --- CORREÇÃO DE TIMEZONE ---
    private Instant genExpirationDate() {
        // Gera data de expiração de forma universal (UTC), somando 8 horas.
        // Funciona independente se o servidor está no Brasil, EUA ou China.
        return Instant.now().plus(8, ChronoUnit.HOURS);
    }
}