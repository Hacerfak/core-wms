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
import org.springframework.beans.factory.annotation.Value; // <--- Import Adicionado
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

    @Value("${api.security.routes.print-agent:/api/impressao/fila}") // <--- Injeção Adicionada
    private String rotaAgente;

    // --- A MÁGICA ACONTECE AQUI ---
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        // Verifica se a requisição tem um Token JWT (Bearer)
        boolean hasToken = authHeader != null && authHeader.startsWith("Bearer ");

        // LÓGICA DE DECISÃO:
        // Se a rota for do Agente (/api/impressao/fila...) E NÃO tiver token,
        // então pulamos este filtro (return true) para deixar o ApiKeyAuthFilter
        // resolver.
        // Se TIVER token (mesmo sendo rota do agente), é o Frontend acessando a
        // listagem,
        // então NÃO pulamos (return false) e deixamos o doFilterInternal autenticar o
        // usuário.
        return path.startsWith(rotaAgente) && !hasToken;
    }

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
                    Usuario usuario = usuarioRepository.findByLoginWithAcessos(login)
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
                    SecurityContextHolder.clearContext();
                }
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