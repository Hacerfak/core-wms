package br.com.hacerfak.coreWMS.modules.seguranca.dto;

public record CriarUsuarioRequest(
                String login,
                String senha,
                Long perfilId,
                Boolean ativo) {
}