package br.com.hacerfak.coreWMS.modules.seguranca.config;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.impressao.domain.AgenteImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.repository.AgenteImpressaoRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.AgenteImpressaoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final AgenteImpressaoRepository agenteRepository;
    private final AgenteImpressaoService agenteService;

    @Value("${api.security.routes.print-agent:/api/impressao/fila}")
    private String rotaProtegidaAgente;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestKey = request.getHeader("X-Agent-Key");
        String tenantId = request.getHeader("X-Tenant-ID");
        String agenteVersao = request.getHeader("X-Agent-Version");

        // Verifica se é uma requisição de Agente
        if (requestKey != null && request.getRequestURI().startsWith(rotaProtegidaAgente)) {

            // 1. MUDANÇA DE CONTEXTO (Multitenant)
            // Se o agente enviou o Tenant, conectamos no banco dele
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setTenant(tenantId);
            }

            try {
                // 2. Validação da Chave (Agora no banco correto!)
                Optional<AgenteImpressao> agenteOpt = agenteRepository.findByApiKeyAndAtivoTrue(requestKey);

                if (agenteOpt.isPresent()) {
                    AgenteImpressao agente = agenteOpt.get();

                    // Autentica no Spring Security
                    var auth = new UsernamePasswordAuthenticationToken(
                            agente.getNome(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_AGENT")));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // Heartbeat
                    agenteService.registrarHeartbeat(agente, agenteVersao);
                } else {
                    log.warn("Tentativa de acesso com chave inválida no tenant: {}", tenantId);
                }
            } catch (Exception e) {
                // Se o tenant não existir ou banco estiver fora, não quebra a requisição,
                // apenas nega auth
                log.error("Erro ao autenticar agente: {}", e.getMessage());
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // CRÍTICO: Limpar o contexto após a requisição para não sujar a thread
            // Apenas se nós definimos (ou o SecurityFilter limpará também, o que é seguro)
            if (requestKey != null) {
                TenantContext.clear();
            }
        }
    }
}