package br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KardexDTO(
        LocalDateTime dataHora,
        String tipoMovimento, // ENTRADA, SAIDA, AJUSTE
        String documento, // LPN, Nota ou Obs
        String usuario,
        BigDecimal entrada,
        BigDecimal saida,
        BigDecimal saldoAnterior,
        BigDecimal saldoAtual) {
}