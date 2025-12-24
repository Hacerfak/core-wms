package br.com.hacerfak.coreWMS.modules.auditoria.service;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.core.util.DiffUtils;
import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import br.com.hacerfak.coreWMS.core.config.MessagingConfig;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final RabbitTemplate rabbitTemplate;
    private final DiffUtils diffUtils;

    /**
     * AUDITORIA AUTOMÁTICA (Smart Diff)
     * Compara o estado antigo com o novo e registra apenas as mudanças.
     * Ideal para: Updates, Creates e Deletes de entidades.
     */
    @Async
    public void registrarAuditoria(String evento, Object entidade, String entidadeId, Object estadoAntigo,
            Object estadoNovo) {
        try {
            // 1. Gera o Diff limpo
            String diff = diffUtils.gerarDiff(estadoAntigo, estadoNovo);

            // Se for UPDATE e nada mudou, ignora (reduz ruído)
            if ("UPDATE".equals(evento) && (diff == null || diff.equals("{}"))) {
                return;
            }

            String nomeEntidade = entidade != null ? entidade.getClass().getSimpleName() : "Desconhecido";
            salvarLog(evento, nomeEntidade, entidadeId, diff);

        } catch (Exception e) {
            log.error("Falha ao salvar auditoria automática", e);
        }
    }

    /**
     * AUDITORIA MANUAL (Eventos de Negócio)
     * Registra uma ação específica onde não houve necessariamente mudança de dados,
     * ou onde queremos gravar uma mensagem específica.
     * Ex: "IMPRESSAO", "LOGIN_FALHA", "GERACAO_RELATORIO"
     */
    @Async
    public void registrarLog(String evento, String entidade, String entidadeId, String mensagemOuDetalhes) {
        try {
            salvarLog(evento, entidade, entidadeId, mensagemOuDetalhes);
        } catch (Exception e) {
            log.error("Falha ao salvar log manual", e);
        }
    }

    // --- Método Privado Centralizado para reaproveitar a captura de contexto ---
    private void salvarLog(String evento, String entidade, String entidadeId, String dados) {
        try {
            // ... (lógica de pegar usuario, ip, tenant igual você já tem) ...
            String tenantId = TenantContext.getTenant();
            if (tenantId == null)
                tenantId = "public";

            AuditLog logEntry = AuditLog.builder()
                    .tenantId(tenantId)
                    .evento(evento)
                    .entidade(entidade)
                    .entidadeId(entidadeId)
                    .usuario(getUsuarioLogado()) // seus métodos helpers
                    .ipOrigem(getIpCliente())
                    .userAgent(getUserAgent())
                    .dados(dados)
                    .build();

            // AGORA ENVIA PARA FILA
            rabbitTemplate.convertAndSend(MessagingConfig.QUEUE_AUDITORIA, logEntry);

        } catch (Exception e) {
            // Fallback: Se o Rabbit falhar, loga erro mas não trava o sistema
            log.error("Erro ao enviar auditoria para fila", e);
        }
    }

    // --- Helpers ---

    private String getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "SISTEMA";
    }

    private String getIpCliente() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwarded = request.getHeader("X-Forwarded-For");
                return xForwarded != null ? xForwarded : request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "DESCONHECIDO";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String ua = attrs.getRequest().getHeader("User-Agent");
                return (ua != null && ua.length() > 250) ? ua.substring(0, 250) : ua;
            }
        } catch (Exception ignored) {
        }
        return "DESCONHECIDO";
    }
}