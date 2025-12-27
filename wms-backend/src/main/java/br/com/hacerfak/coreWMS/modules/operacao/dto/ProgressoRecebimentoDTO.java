package br.com.hacerfak.coreWMS.modules.operacao.dto;

import java.math.BigDecimal;

public record ProgressoRecebimentoDTO(
        Long solicitacaoId,
        BigDecimal totalPrevisto,
        BigDecimal totalConferido) {
}