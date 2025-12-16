package br.com.hacerfak.coreWMS.modules.cadastro.dto;

import java.math.BigDecimal;

// Record: Uma classe imutável, perfeita para transportar dados
public record ProdutoRequest(
        String nome,
        String sku,
        String ean13,
        String unidadeMedida,
        BigDecimal pesoBrutoKg,
        String ncm) {
}
