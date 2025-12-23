package br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio;

public record OcupacaoDTO(
        String area,
        String tipoEstrutura,

        Long totalEnderecosChao, // Quantos "quadrados" no chão temos
        Long capacidadePalletsTotal, // Quantos pallets caberiam no mundo ideal (tudo Suco)
        Long capacidadePalletsReal, // Quantos cabem HOJE considerando que tem Vinho estocado

        Long palletsFisicos, // Quantidade real estocada
        Double taxaOcupacaoReal, // palletsFisicos / capacidadePalletsReal

        Long perdaCapacidade // Quanto espaço perdi por colocar vinho (capacidadeTotal - capacidadeReal)
) {
}