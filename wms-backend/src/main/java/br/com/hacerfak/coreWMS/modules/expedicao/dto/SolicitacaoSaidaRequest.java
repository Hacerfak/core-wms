package br.com.hacerfak.coreWMS.modules.expedicao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SolicitacaoSaidaRequest(
        @NotBlank String codigoExterno,
        @NotNull Long clienteId,
        Integer prioridade,
        LocalDateTime dataLimite,
        String rota,
        Integer sequenciaEntrega,
        @NotEmpty List<ItemSolicitacaoRequest> itens) {
    public record ItemSolicitacaoRequest(
            @NotNull Long produtoId,
            @NotNull @Positive BigDecimal quantidade) {
    }
}