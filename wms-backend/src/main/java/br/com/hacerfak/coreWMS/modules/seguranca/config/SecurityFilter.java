package br.com.hacerfak.coreWMS.modules.seguranca.config;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.UserRole;
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

                    // --- CORREÇÃO DE SEGURANÇA CRÍTICA ---
                    // Se não tem tenant no token, cai no Default (Master).
                    // MAS usuários comuns NÃO podem acessar o Master, apenas ADMIN.
                    if (tenantId != null) {
                        TenantContext.setTenant(tenantId);
                    } else {
                        // Tentativa de acesso ao MASTER
                        if (usuario.getRole() != UserRole.ADMIN) {
                            // Bloqueia silenciosamente ou lança erro. Aqui vamos logar e não autenticar.
                            System.out.println(">>> ALERTA SEGURANÇA: Usuário " + login
                                    + " tentou acessar contexto MASTER sem permissão.");
                            throw new RuntimeException("Acesso negado ao contexto Global.");
                        }
                        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
                    }

                    var authentication = new UsernamePasswordAuthenticationToken(usuario, null,
                            usuario.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } catch (Exception e) {
                    System.out.println(">>> ERRO DE AUTENTICAÇÃO: " + e.getMessage());
                    // Contexto limpo, Security retornará 403
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