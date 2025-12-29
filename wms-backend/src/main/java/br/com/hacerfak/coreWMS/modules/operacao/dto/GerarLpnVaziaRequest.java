package br.com.hacerfak.coreWMS.modules.operacao.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GerarLpnVaziaRequest(
        @NotNull @Min(1) Integer quantidade,
        @NotNull Long formatoId) {
}