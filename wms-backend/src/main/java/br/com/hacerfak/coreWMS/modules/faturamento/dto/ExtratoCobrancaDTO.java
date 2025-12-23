package br.com.hacerfak.coreWMS.modules.faturamento.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExtratoCobrancaDTO(
        String nomeCliente,
        String cnpjCliente,
        LocalDate dataInicio,
        LocalDate dataFim,
        BigDecimal valorTotalPeriodo,
        List<ItemExtratoDTO> itens) {
}