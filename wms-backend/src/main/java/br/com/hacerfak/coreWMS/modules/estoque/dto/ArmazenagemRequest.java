package br.com.hacerfak.coreWMS.modules.estoque.dto;

import jakarta.validation.constraints.NotNull;

public record ArmazenagemRequest(
        @NotNull(message = "O LPN é obrigatório") String lpn,

        @NotNull(message = "O ID do local de destino é obrigatório") Long localDestinoId) {
}