package br.com.hacerfak.coreWMS.modules.seguranca.config;

import br.com.hacerfak.coreWMS.modules.impressao.domain.AgenteImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.repository.AgenteImpressaoRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.AgenteImpressaoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final AgenteImpressaoRepository agenteRepository;
    private final AgenteImpressaoService agenteService;
    // Injeta valor do application.yaml (com fallback para o padrão antigo se
    // falhar)
    @Value("${api.security.routes.print-agent:/api/impressao/fila}")
    private String rotaProtegidaAgente;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestKey = request.getHeader("X-API-KEY");

        // Só processa se tiver a chave e for rota de impressão
        if (requestKey != null && request.getRequestURI().startsWith(rotaProtegidaAgente)) {

            // Busca no banco (Cacheado)
            Optional<AgenteImpressao> agenteOpt = agenteRepository.findByApiKeyAndAtivoTrue(requestKey);

            if (agenteOpt.isPresent()) {
                AgenteImpressao agente = agenteOpt.get();

                // Autentica o agente no Contexto do Spring Security
                var auth = new UsernamePasswordAuthenticationToken(
                        agente.getNome(), // Principal (Nome do Agente)
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_AGENT")));
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Opcional: Atualizar heartbeat assincronamente
                agenteService.registrarHeartbeat(agente);
            }
        }

        filterChain.doFilter(request, response);
    }
}