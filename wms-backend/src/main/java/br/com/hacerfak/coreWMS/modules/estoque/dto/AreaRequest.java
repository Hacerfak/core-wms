package br.com.hacerfak.coreWMS.modules.estoque.dto;

import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoLocalizacao;

public record AreaRequest(
                Long id,
                Long armazemId,
                String codigo,
                String nome,
                TipoLocalizacao tipo,
                Boolean padraoRecebimento,
                Boolean padraoExpedicao,
                Boolean padraoQuarentena,
                Boolean ativo) {
}