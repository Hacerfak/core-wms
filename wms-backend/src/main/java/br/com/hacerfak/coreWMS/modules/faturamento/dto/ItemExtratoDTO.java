package br.com.hacerfak.coreWMS.modules.faturamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemExtratoDTO(
        LocalDate data,
        String nomeServico,
        String unidadeMedida,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal,
        String referencia // Ex: Onda 100, NF 500
) {
}