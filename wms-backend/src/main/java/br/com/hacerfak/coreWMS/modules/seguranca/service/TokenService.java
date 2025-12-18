package br.com.hacerfak.coreWMS.modules.seguranca.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
                    .withExpiresAt(genExpirationDate());

            if (tenantId != null)
                builder.withClaim("tenant", tenantId);

            // Adiciona as permissões ao Token
            if (authorities != null && !authorities.isEmpty()) {
                builder.withClaim("roles", authorities);
            }

            return builder.sign(algorithm);
        } catch (Exception exception) {
            throw new RuntimeException("Erro ao gerar token", exception);
        }
    }
    // ... (restante dos métodos validateToken, etc. mantém igual)

    private Instant genExpirationDate() {
        return LocalDateTime.now().plusHours(8).toInstant(ZoneOffset.of("-03:00"));
    }

    // Método auxiliar (já existe, mas mantenha)
    public String getTenantFromToken(String token) {
        /* ... */ return null;
    } // Mantenha sua implementação

    public String validateToken(String token) {
        /* ... */ return "";
    } // Mantenha sua implementação
}