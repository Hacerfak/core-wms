package br.com.hacerfak.coreWMS.modules.seguranca.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationDTO(
        @NotBlank(message = "O login é obrigatório") String login,

        @NotBlank(message = "A senha é obrigatória") String password) {
}
