package br.com.hacerfak.coreWMS.modules.portaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record TurnoRequest(
        @NotBlank String nome,
        @NotNull LocalTime inicio,
        @NotNull LocalTime fim,
        String diasSemana, // Ex: "SEG,TER,QUA..."
        Boolean ativo) {
}