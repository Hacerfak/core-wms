package br.com.hacerfak.coreWMS.modules.seguranca.dto;

public record VerificarUsuarioDTO(
        boolean existe,
        Long id,
        String login) {
}