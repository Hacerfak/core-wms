package br.com.hacerfak.coreWMS.modules.estoque.dto;

import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoEstrutura;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoLocalizacao;
import java.math.BigDecimal;

public record LocalizacaoRequest(
        Long id,
        Long areaId,
        String codigo,
        String descricao,
        TipoLocalizacao tipo,
        Boolean virtual,
        Boolean permiteMultiLpn,
        Integer capacidadeLpn,
        BigDecimal capacidadePesoKg,
        Boolean bloqueado,
        TipoEstrutura tipoEstrutura,
        Integer capacidadeMaxima,
        Boolean ativo) {
}