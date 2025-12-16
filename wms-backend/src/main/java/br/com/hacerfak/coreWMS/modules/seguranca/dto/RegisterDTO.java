package br.com.hacerfak.coreWMS.modules.seguranca.dto;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDTO(
        @NotBlank(message = "O login é obrigatório") String login,

        @NotBlank(message = "A senha é obrigatória") String password,

        @NotNull(message = "O perfil de acesso (role) é obrigatório") UserRole role) {
}