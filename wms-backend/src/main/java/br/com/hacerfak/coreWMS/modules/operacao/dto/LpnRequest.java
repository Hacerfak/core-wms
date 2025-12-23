package br.com.hacerfak.coreWMS.modules.operacao.dto;

import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoLpn;
import jakarta.validation.constraints.NotNull;

public record LpnRequest(
        // Se nulo, sistema gera. Se enviado, usa o código bipado da etiqueta
        // pré-impressa.
        String codigoLpn,

        @NotNull TipoLpn tipoLpn) {
}