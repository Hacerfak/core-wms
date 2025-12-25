package br.com.hacerfak.coreWMS.modules.portaria.dto;

import br.com.hacerfak.coreWMS.modules.portaria.domain.TipoAgendamento;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AgendamentoRequest(
        @NotNull TipoAgendamento tipo,
        @NotNull LocalDateTime dataInicio,
        @NotNull LocalDateTime dataFim,

        Long transportadoraId,
        Long motoristaId,
        String placa,
        Long docaId,
        Long turnoId,

        // Novos Campos
        Long solicitacaoSaidaId,
        Long depositanteId // Opcional para entrada manual
) {
}