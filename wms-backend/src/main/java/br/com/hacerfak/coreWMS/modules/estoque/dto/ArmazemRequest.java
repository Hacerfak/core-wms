package br.com.hacerfak.coreWMS.modules.estoque.dto;

public record ArmazemRequest(
        Long id,
        String codigo,
        String nome,
        String enderecoCompleto,
        Boolean ativo) {
}