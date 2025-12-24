package br.com.hacerfak.coreWMS.modules.impressao.dto;

import jakarta.validation.constraints.NotBlank;

public record AgenteRequest(
        @NotBlank String nome,
        String descricao,
        String hostname) {
}