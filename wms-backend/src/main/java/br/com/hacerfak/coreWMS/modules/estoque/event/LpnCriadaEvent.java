package br.com.hacerfak.coreWMS.modules.estoque.event;

public record LpnCriadaEvent(
        Long lpnId,
        String codigoLpn,
        String tenantId) {
}