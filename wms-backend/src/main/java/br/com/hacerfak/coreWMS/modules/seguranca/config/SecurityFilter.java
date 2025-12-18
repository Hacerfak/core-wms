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

        // Ignora requisições OPTIONS (Preflight) no filtro
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
                        TenantContext.setTenant(tenantId);
                    } else {
                        TenantContext.setTenant(TenantContext.DEFAULT_TENANT_ID);
                    }

                    var authentication = new UsernamePasswordAuthenticationToken(usuario, null,
                            usuario.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                } catch (Exception e) {
                    System.out.println(">>> ERRO DE AUTENTICAÇÃO: " + e.getMessage());
                    // Não lançamos erro aqui para deixar o Spring Security retornar 403 padrão se
                    // falhar
                }
            } else {
                System.out.println(">>> TOKEN INVÁLIDO OU EXPIRADO");
            }
        } else {
            System.out.println(">>> TOKEN NÃO ENCONTRADO NO HEADER"); // Descomente para
            // debug severo
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