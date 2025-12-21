package br.com.hacerfak.coreWMS.core.listener;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.core.util.BeanUtil;
import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import br.com.hacerfak.coreWMS.modules.auditoria.service.AuditService;
import jakarta.persistence.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

public class GlobalAuditListener {

    @PostPersist
    public void onPostPersist(Object entity) {
        enviarLog("INSERT", entity);
    }

    @PostUpdate
    public void onPostUpdate(Object entity) {
        enviarLog("UPDATE", entity);
    }

    @PostRemove
    public void onPostRemove(Object entity) {
        enviarLog("DELETE", entity);
    }

    private void enviarLog(String action, Object entity) {
        try {
            // 1. Identificar Usuário
            String usuario = "SISTEMA";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                usuario = auth.getName();
            }

            // 2. Identificar Tenant
            String tenant = TenantContext.getTenant();

            // 3. Pegar o ID da entidade (Reflexão básica ou assumindo BaseEntity)
            String entityId = "N/A";
            try {
                // Tenta chamar o método getId() se existir
                entityId = String.valueOf(entity.getClass().getMethod("getId").invoke(entity));
            } catch (Exception e) {
                // Ignora se não tiver getId
            }

            // 4. Montar o Objeto de Log
            AuditLog log = AuditLog.builder()
                    .entityName(entity.getClass().getSimpleName())
                    .entityId(entityId)
                    .action(action)
                    .tenantId(tenant)
                    .usuario(usuario)
                    .dataHora(LocalDateTime.now())
                    .conteudo(entity) // O Mongo serializa o objeto inteiro como JSON
                    .build();

            // 5. Enviar para o Serviço Assíncrono
            // Como esta classe não é um Bean Spring, usamos o BeanUtil para pegar o service
            AuditService auditService = BeanUtil.getBean(AuditService.class);
            auditService.registrarLog(log);

        } catch (Exception e) {
            System.err.println("Erro ao preparar log de auditoria: " + e.getMessage());
        }
    }
}