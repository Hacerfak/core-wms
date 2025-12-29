package br.com.hacerfak.coreWMS.modules.operacao.dto;

import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusQualidade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;

public record GerarLpnMassaRequest(
        @NotNull String sku,
        @NotNull @Positive BigDecimal qtdPorVolume,
        @NotNull @Min(1) Integer qtdVolumes,
        @NotNull Long formatoId,
        Long localizacaoId, // Onde o estoque vai nascer
        Long solicitacaoId,
        String lote,
        LocalDate dataValidade,
        String numeroSerie,
        StatusQualidade statusQualidade) {
}