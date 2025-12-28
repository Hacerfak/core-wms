package br.com.hacerfak.coreWMS.modules.portaria.listener;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.operacao.event.EntradaFinalizadaEvent;
import br.com.hacerfak.coreWMS.modules.portaria.service.PortariaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortariaInboundListener {

    private final PortariaService portariaService;

    /**
     * Reage quando uma entrada é finalizada no WMS.
     * Objetivo: Liberar a doca e mover o caminhão para status 'AGUARDANDO_SAIDA'.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEntradaFinalizada(EntradaFinalizadaEvent event) {
        // Configura o Tenant na thread assíncrona
        TenantContext.setTenant(event.tenantId());

        try {
            log.info("Portaria: Processando liberação de doca para Solicitação #{}", event.solicitacaoId());
            portariaService.liberarDocaPorSolicitacao(event.solicitacaoId());
        } catch (Exception e) {
            log.error("Erro ao liberar doca automaticamente para solicitação {}", event.solicitacaoId(), e);
        } finally {
            TenantContext.clear();
        }
    }
}