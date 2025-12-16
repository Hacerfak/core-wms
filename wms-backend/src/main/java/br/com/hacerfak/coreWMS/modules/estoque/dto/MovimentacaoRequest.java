package br.com.hacerfak.coreWMS.modules.estoque.dto;

import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoMovimento;
import java.math.BigDecimal;

public record MovimentacaoRequest(
        Long produtoId,
        Long localizacaoId,
        BigDecimal quantidade,
        String lote,
        String numeroSerie,
        TipoMovimento tipo,
        String observacao) {
}
