package br.com.hacerfak.coreWMS.modules.operacao.listener;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.operacao.event.EntradaCriadaEvent;
import br.com.hacerfak.coreWMS.modules.operacao.service.RecebimentoWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecebimentoListener {

    private final RecebimentoWorkflowService workflowService;

    // Escuta após o commit do banco para garantir que o ID existe
    @Async // Executa em thread separada
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSolicitacaoCriada(EntradaCriadaEvent event) {
        // Em threads async, precisamos setar o Tenant manualmente
        TenantContext.setTenant(event.tenantId());
        try {
            log.info("Iniciando processamento assíncrono da Entrada #{}", event.solicitacaoId());
            workflowService.processarInicioEntrada(event.solicitacaoId());
        } catch (Exception e) {
            log.error("Erro no processamento async da entrada {}", event.solicitacaoId(), e);
            // Em produção: aqui implementaria uma lógica de Retry ou Dead Letter Queue
        } finally {
            TenantContext.clear();
        }
    }
}