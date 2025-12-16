package br.com.hacerfak.coreWMS.modules.operacao.dto;

import java.math.BigDecimal;

public record VolumeResumoDTO(
        Long id,
        String lpn,
        BigDecimal quantidadeOriginal) {
}