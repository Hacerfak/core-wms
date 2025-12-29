package br.com.hacerfak.coreWMS.modules.estoque.dto;

import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoEstrutura;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoLocalizacao;
import java.math.BigDecimal;
import java.util.List;

public record LocalizacaoBulkUpdateDTO(
        List<Long> ids,
        // Campos opcionais (se null, não altera)
        TipoLocalizacao tipo,
        TipoEstrutura estrutura,
        Integer capacidadeLpn,
        BigDecimal capacidadePeso,
        Integer capacidadeMaxima, // Empilhamento
        Boolean ativo,
        Boolean bloqueado,
        Boolean virtualLocation,
        Boolean permiteMultiLpn) {
}