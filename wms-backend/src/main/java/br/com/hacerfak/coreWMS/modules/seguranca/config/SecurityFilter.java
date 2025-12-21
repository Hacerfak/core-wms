package br.com.hacerfak.coreWMS.modules.seguranca.config;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getMethod().equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = this.recoverToken(request);

        if (token != null) {
            var login = tokenService.validateToken(token);

            if (!login.isEmpty()) {
                try {
                    Usuario usuario = usuarioRepository.findByLogin(login)
                            .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + login));

                    String tenantId = tokenService.getTenantFromToken(token);

                    if (tenantId != null) {
                        // Token de acesso a uma empresa específica
                        TenantContext.setTenant(tenantId);
                    } else {
                        // Token de Login Inicial (Sem Tenant)
                        // CORREÇÃO: Permitimos o acesso ao contexto Master (Lobby) para TODOS os
                        // usuários autenticados.
                        // Isso é necessário para que eles possam consultar a lista de empresas
                        // (endpoint /meus-acessos).
                        // A segurança dos dados sensíveis do Master continua garantida pelo
                        // @PreAuthorize nos Controllers.
                        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
                    }

                    var authentication = new UsernamePasswordAuthenticationToken(usuario, null,
                            usuario.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } catch (Exception e) {
                    System.out.println(">>> ERRO DE AUTENTICAÇÃO: " + e.getMessage());
                    // Limpa o contexto para garantir que não haja vazamento de segurança em caso de
                    // erro
                    SecurityContextHolder.clearContext();
                }
            } else {
                System.out.println(">>> TOKEN INVÁLIDO OU EXPIRADO");
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null)
            return null;
        return authHeader.replace("Bearer ", "");
    }
}