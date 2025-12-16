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
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret:meu-segredo-super-secreto}")
    private String secret;

    /**
     * Gera um token. Se tenantId for passado, ele é gravado no token.
     */
    public String generateToken(Usuario usuario, String tenantId) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            var jwtBuilder = JWT.create()
                    .withIssuer("wms-api")
                    .withSubject(usuario.getLogin())
                    .withExpiresAt(genExpirationDate());

            // Se o usuário selecionou uma empresa, adicionamos isso ao Token
            if (tenantId != null) {
                jwtBuilder.withClaim("tenant", tenantId);
            }

            return jwtBuilder.sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    /**
     * Valida o token e retorna o LOGIN (Subject)
     */
    public String validateToken(String token) {
        try {
            return getDecodedJWT(token).getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    /**
     * Extrai o ID da Empresa (Tenant) de dentro do Token
     */
    public String getTenantFromToken(String token) {
        try {
            DecodedJWT jwt = getDecodedJWT(token);
            // Retorna nulo se o claim não existir (token de login inicial)
            if (jwt.getClaim("tenant").isNull()) {
                return null;
            }
            return jwt.getClaim("tenant").asString();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    private DecodedJWT getDecodedJWT(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.require(algorithm)
                .withIssuer("wms-api")
                .build()
                .verify(token);
    }

    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(8).toInstant(ZoneOffset.of("-03:00"));
    }
}