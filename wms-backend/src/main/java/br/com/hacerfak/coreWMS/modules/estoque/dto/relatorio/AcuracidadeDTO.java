package br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AcuracidadeDTO(
        Long inventarioId,
        String descricao,
        LocalDate data,
        Long totalContagens,
        Long contagensCorretas,
        Long contagensDivergentes,
        BigDecimal acuracidadePercentual // (Corretas / Total) * 100
) {
}