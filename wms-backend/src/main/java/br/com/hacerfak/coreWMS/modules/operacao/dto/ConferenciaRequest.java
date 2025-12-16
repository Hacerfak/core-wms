package br.com.hacerfak.coreWMS.modules.operacao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ConferenciaRequest(
        @NotNull String sku,

        @NotNull @Positive BigDecimal quantidade, // Quantidade de produtos DENTRO deste volume/pallet

        // Opcional: Se o fornecedor já mandou uma etiqueta dele, usamos ela.
        // Se vier nulo, nós geramos o LPN nosso.
        String lpnExterno) {
}