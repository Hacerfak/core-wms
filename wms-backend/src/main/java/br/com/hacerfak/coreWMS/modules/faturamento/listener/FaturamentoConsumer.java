package br.com.hacerfak.coreWMS.modules.faturamento.listener;

import br.com.hacerfak.coreWMS.core.config.MessagingConfig;
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext; // Importante
import br.com.hacerfak.coreWMS.modules.faturamento.dto.FaturamentoEvent;
import br.com.hacerfak.coreWMS.modules.faturamento.service.FaturamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FaturamentoConsumer {

    private final FaturamentoService faturamentoService;

    @RabbitListener(queues = MessagingConfig.QUEUE_FATURAMENTO)
    public void processarFaturamento(FaturamentoEvent evento) {
        try {
            // 1. Define o contexto do Tenant para esta Thread do RabbitMQ
            // Isso garante que o EntityManager conecte no Schema correto do PostgreSQL
            if (evento.tenantId() != null) {
                TenantContext.setTenant(evento.tenantId());
            }

            log.info("Processando faturamento no contexto: {}", evento.tenantId());

            // 2. Executa a lógica de negócio
            faturamentoService.calcularServicosEntrada(evento.solicitacaoId());

        } catch (Exception e) {
            log.error("Erro ao processar faturamento ID {}: {}", evento.solicitacaoId(), e.getMessage());
            // Opcional: throw e; para reenfileirar em caso de erro transiente
        } finally {
            // 3. Limpeza OBRIGATÓRIA para não poluir a thread (Thread Pooling)
            TenantContext.clear();
        }
    }
}