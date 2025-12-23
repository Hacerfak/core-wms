package br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio;

import java.math.BigDecimal;

public record PosicaoEstoqueDTO(
        String codigoProduto,
        String descricaoProduto,
        String endereco,
        String lpn,
        String lote,
        String validade, // Estamos tratando como String pois fazemos CAST no SQL
        String statusQualidade, // Estamos tratando como String vindo do banco
        BigDecimal quantidade) {
}