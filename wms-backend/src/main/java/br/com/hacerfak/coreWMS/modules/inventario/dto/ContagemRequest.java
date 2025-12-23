package br.com.hacerfak.coreWMS.modules.inventario.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ContagemRequest(
        @NotNull @PositiveOrZero BigDecimal quantidade,
        Long produtoId // Opcional, se o operador identificar produto no local
) {
}