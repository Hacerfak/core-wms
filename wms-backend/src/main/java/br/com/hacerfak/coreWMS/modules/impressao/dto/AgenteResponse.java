package br.com.hacerfak.coreWMS.modules.impressao.dto;

import java.time.LocalDateTime;

public record AgenteResponse(
        Long id,
        String nome,
        String descricao,
        String apiKey, // Retornada apenas na criação ou para admins
        boolean ativo,
        LocalDateTime ultimoHeartbeat,
        String versaoAgente,
        String statusConexao // "ONLINE" ou "OFFLINE" baseado no heartbeat
) {
}