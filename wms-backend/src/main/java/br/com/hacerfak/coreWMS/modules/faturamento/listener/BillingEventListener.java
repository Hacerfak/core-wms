package br.com.hacerfak.coreWMS.modules.faturamento.listener;

import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.event.EstoqueMovimentadoEvent;
import br.com.hacerfak.coreWMS.modules.faturamento.service.FaturamentoService;
import br.com.hacerfak.coreWMS.modules.operacao.event.EntradaFinalizadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class BillingEventListener {

    private final FaturamentoService faturamentoService;
    private final ProdutoRepository produtoRepository;

    /**
     * Captura Picking (Saída de Estoque)
     * Cobra por UNIDADE separada.
     */
    @Async
    @EventListener
    public void handleEstoqueMovimentado(EstoqueMovimentadoEvent event) {
        // Apenas movimentos de SAÍDA geram cobrança de Picking
        // (Movimentação interna não gera, Entrada gera via outro evento)
        if ("SAIDA".equals(event.tipoMovimento())) {

            // Busca o produto para saber quem é o dono (Depositante)
            produtoRepository.findById(event.produtoId()).ifPresent(produto -> {
                if (produto.getDepositante() != null) {
                    faturamentoService.registrarCobrancaAutomatica(
                            produto.getDepositante(),
                            "PICKING_UNIDADE",
                            event.quantidadeMovimentada(),
                            "Picking Auto - Loc " + event.localizacaoId());
                }
            });
        }
    }

    /**
     * Captura Recebimento (Entrada de Nota Fiscal)
     * Cobra por DOCUMENTO (Nota Fiscal) recebido.
     */
    @Async
    @EventListener
    public void handleEntradaFinalizada(EntradaFinalizadaEvent event) {
        // Verifica se a solicitação tem um fornecedor ou cliente vinculado
        // No WMS 3PL, geralmente cobramos do cliente dono da mercadoria, não do
        // fornecedor que entregou.
        // A SolicitacaoEntrada deve ter o vínculo lógico. Assumindo que cobramos o
        // parceiro da nota.

        // Se a entidade SolicitacaoEntrada tiver um campo "Dono/Cliente", usamos ele.
        // Como no MVP usamos "Fornecedor", vamos cobrar dele ou precisaríamos ajustar o
        // modelo
        // para ter "Depositante" na capa da entrada.
        // Lógica simplificada: Cobra do fornecedor (cenário de logística reversa) ou
        // ignora se for compra própria.

        if (event.solicitacao().getFornecedor() != null) {
            faturamentoService.registrarCobrancaAutomatica(
                    event.solicitacao().getFornecedor(),
                    "RECEBIMENTO_NF",
                    BigDecimal.ONE, // 1 Nota Fiscal
                    "Recebimento NF " + event.notaFiscal());
        }
    }
}