package br.com.hacerfak.coreWMS.modules.estoque.listener;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.estoque.service.ArmazenagemWorkflowService;
import br.com.hacerfak.coreWMS.modules.operacao.event.EntradaFinalizadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EstoqueInboundListener {

    private final ArmazenagemWorkflowService armazenagemService;

    // Escuta o evento APÓS o commit da transação de entrada ter sucesso
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async // Executa em thread separada para não travar a resposta do Controller
    public void onEntradaFinalizada(EntradaFinalizadaEvent event) {
        // Como é async, precisamos configurar o Tenant manualmente na nova thread
        TenantContext.setTenant(event.tenantId());

        try {
            System.out.println(">>> ESTOQUE: Recebido evento de entrada finalizada ID: " + event.solicitacaoId());

            // Dispara a geração de tarefas de armazenagem (Put-away)
            armazenagemService.gerarTarefasDeArmazenagem(event.solicitacaoId());

        } catch (Exception e) {
            System.err.println("ERRO AO GERAR ARMAZENAGEM: " + e.getMessage());
            e.printStackTrace();
        } finally {
            TenantContext.clear();
        }
    }
}