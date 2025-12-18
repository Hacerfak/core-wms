package br.com.hacerfak.coreWMS.modules.seguranca.dto;

public record UsuarioDTO(
        Long id,
        String login,
        String perfilNome,
        boolean ativo) {
}