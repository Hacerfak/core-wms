package br.com.hacerfak.coreWMS.modules.estoque.event;

import java.math.BigDecimal;

public record EstoqueMovimentadoEvent(
        Long produtoId,
        Long localizacaoId,
        BigDecimal quantidadeMovimentada,
        BigDecimal saldoResultante,
        String tipoMovimento // ENTRADA, SAIDA, AJUSTE...
) {
}