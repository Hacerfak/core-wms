package br.com.hacerfak.coreWMS.modules.cadastro.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank(message = "O SKU é obrigatório") String sku,

        @NotBlank(message = "O nome é obrigatório") String nome,

        String ean13,
        String dun14,

        @NotBlank(message = "A unidade de medida é obrigatória") String unidadeMedida, // UN, CX, KG

        BigDecimal pesoBrutoKg,
        String ncm,
        String cest,
        BigDecimal valorUnitarioPadrao,

        // Regras
        Boolean ativo,
        Boolean controlaLote,
        Boolean controlaValidade,
        Boolean controlaSerie,

        // Conversão
        String unidadeArmazenagem,
        Integer fatorConversao,
        Integer fatorEmpilhamento) {
}