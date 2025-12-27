package br.com.hacerfak.coreWMS.modules.operacao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusQualidade;

public record GerarLpnMassaRequest(
                @NotNull Long solicitacaoId, // Vincula à entrada
                @NotNull String sku,
                @NotNull @Positive BigDecimal quantidadePorVolume, // Ex: 50 caixas por pallet
                @NotNull @Positive Integer quantidadeDeVolumes, // Ex: 500 pallets

                String lote,
                LocalDate dataValidade,
                String numeroSerie,

                StatusQualidade statusQualidade) {
}