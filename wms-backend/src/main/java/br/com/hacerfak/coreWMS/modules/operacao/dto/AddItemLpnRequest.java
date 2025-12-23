package br.com.hacerfak.coreWMS.modules.operacao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusQualidade;

public record AddItemLpnRequest(
        @NotNull String sku,
        @NotNull @Positive BigDecimal quantidade,
        String lote,
        LocalDate dataValidade,
        String numeroSerie,
        StatusQualidade statusQualidade) {
}