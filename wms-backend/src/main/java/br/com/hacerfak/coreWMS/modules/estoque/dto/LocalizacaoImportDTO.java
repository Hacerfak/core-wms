package br.com.hacerfak.coreWMS.modules.estoque.dto;

import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoEstrutura;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoLocalizacao;
import java.math.BigDecimal;

public record LocalizacaoImportDTO(
        String codigoArmazem,
        String codigoArea,
        String codigoLocal, // Sufixo
        String descricao,
        TipoLocalizacao tipo, // PICKING, PULMAO, etc
        TipoEstrutura estrutura, // PORTA_PALLET, BLOCADO
        Integer capacidadeLpn,
        Integer capacidadeMaxima,
        BigDecimal capacidadePeso,
        Boolean virtual,
        Boolean multiLpn,
        Boolean bloqueado,
        Boolean ativo) {
}