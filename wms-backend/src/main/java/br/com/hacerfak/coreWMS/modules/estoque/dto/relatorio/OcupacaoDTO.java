package br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio;

public record OcupacaoDTO(
        String area,
        String tipoLocal, // PULMAO, PICKING
        Long totalPosicoes,
        Long posicoesOcupadas,
        Long posicoesVazias,
        Double taxaOcupacao // %
) {
}