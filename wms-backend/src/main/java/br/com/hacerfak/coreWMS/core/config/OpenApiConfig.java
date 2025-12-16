package br.com.hacerfak.coreWMS.core.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "WMS Core API", version = "1.0", description = "Documentação da API do Sistema de Gestão de Armazém", contact = @Contact(name = "Suporte WMS", email = "suporte@seuwms.com.br")),
        // ISTO AQUI FAZ O CADEADO APARECER GLOBALMENTE
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", description = "Autenticação JWT padrão. Faça login em /auth/login para pegar o token.", scheme = "bearer", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER)
public class OpenApiConfig {
}
