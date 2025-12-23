package br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AgingDTO(
        String sku,
        String produto,
        String lote,
        LocalDate dataValidade,
        Integer diasParaVencer,
        BigDecimal quantidade,
        String statusQualidade,
        String faixaRisco // "CRITICO" (<30 dias), "ALERTA" (30-60), "OK"
) {
}