package br.com.hacerfak.coreWMS.modules.faturamento.dto;

import java.io.Serializable;

public record FaturamentoEvent(Long solicitacaoId, String tenantId) implements Serializable {
}