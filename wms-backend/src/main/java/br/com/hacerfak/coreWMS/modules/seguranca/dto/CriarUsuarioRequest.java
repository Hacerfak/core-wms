package br.com.hacerfak.coreWMS.modules.seguranca.dto;

public record CriarUsuarioRequest(
        String nome,
        String login,
        String email,
        String senha,
        Boolean ativo) {
}