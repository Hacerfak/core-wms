package br.com.hacerfak.coreWMS.modules.seguranca.dto;

public record UsuarioDTO(
                Long id,
                String login,
                String email,
                String nome,
                String role, // Ex: "MASTER" ou "Usuário Comum"
                boolean ativo,
                boolean adminMaster // Se é admin global
) {
}