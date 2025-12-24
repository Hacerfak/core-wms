package br.com.hacerfak.coreWMS.core.listener;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.core.util.BeanUtil;
import br.com.hacerfak.coreWMS.modules.auditoria.service.AuditService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class GlobalAuditListener {

    @PostPersist
    public void onPersist(Object entity) {
        enviarParaAuditoria("CREATE", null, entity);
    }

    @PostUpdate
    public void onUpdate(Object entity) {
        enviarParaAuditoria("UPDATE", null, entity);
    }

    @PostRemove
    public void onRemove(Object entity) {
        enviarParaAuditoria("DELETE", entity, null);
    }

    private void enviarParaAuditoria(String evento, Object antigo, Object novo) {
        try {
            AuditService auditService = BeanUtil.getBean(AuditService.class);
            if (auditService == null)
                return;

            Object alvo = (novo != null) ? novo : antigo;
            String entidadeId = safeGetId(alvo);

            // --- CAPTURA DE CONTEXTO (THREAD PRINCIPAL) ---
            // Capturamos aqui pois na thread @Async esses valores estarão nulos
            String tenantId = TenantContext.getTenant();
            String usuario = getUsuarioLogado();
            String ip = getIpCliente();
            String ua = getUserAgent();

            // Chama o serviço passando os metadados já resolvidos
            auditService.registrarAuditoria(evento, alvo, entidadeId, antigo, novo, tenantId, usuario, ip, ua);

        } catch (Exception e) {
            log.error("Erro ao processar auditoria: " + e.getMessage());
        }
    }

    // --- Helpers de Extração ---

    private String safeGetId(Object entity) {
        if (entity == null)
            return "N/A";
        try {
            return String.valueOf(entity.getClass().getMethod("getId").invoke(entity));
        } catch (Exception e) {
            return "ID_DESCONHECIDO";
        }
    }

    private String getUsuarioLogado() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null && auth.isAuthenticated()) ? auth.getName() : "SISTEMA";
        } catch (Exception e) {
            return "SISTEMA";
        }
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