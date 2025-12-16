package br.com.hacerfak.coreWMS.modules.seguranca.dto;

import jakarta.validation.constraints.NotBlank;

public record SelecaoEmpresaDTO(
        @NotBlank String tenantId) {
}