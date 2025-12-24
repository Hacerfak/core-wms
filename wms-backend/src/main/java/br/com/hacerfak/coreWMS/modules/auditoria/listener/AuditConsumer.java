package br.com.hacerfak.coreWMS.modules.auditoria.listener;

import br.com.hacerfak.coreWMS.core.config.MessagingConfig;
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import br.com.hacerfak.coreWMS.modules.auditoria.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuditConsumer {

    private final AuditLogRepository repository;

    @RabbitListener(queues = MessagingConfig.QUEUE_AUDITORIA)
    public void consumirLog(AuditLog logEntry) {
        try {
            // 1. RE-HIDRATAR O CONTEXTO DO TENANT
            if (logEntry.getTenantId() != null) {
                TenantContext.setTenant(logEntry.getTenantId());
            }

            // 2. Salvar (Agora o Hibernate sabe qual schema usar!)
            repository.save(logEntry);

        } catch (Exception e) {
            log.error("Erro ao salvar auditoria", e);
        } finally {
            // 3. Limpar sempre para não sujar a thread do pool
            TenantContext.clear();
        }
    }
}