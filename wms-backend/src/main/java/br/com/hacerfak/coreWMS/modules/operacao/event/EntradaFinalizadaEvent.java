package br.com.hacerfak.coreWMS.modules.operacao.event;

public record EntradaFinalizadaEvent(
        Long solicitacaoId,
        String tenantId,
        String usuarioResponsavel) {
}