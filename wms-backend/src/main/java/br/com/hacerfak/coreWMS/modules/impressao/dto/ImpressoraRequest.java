package br.com.hacerfak.coreWMS.modules.impressao.dto;

import br.com.hacerfak.coreWMS.modules.impressao.domain.TipoConexaoImpressora;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImpressoraRequest(
        @NotBlank String nome,
        String descricao,
        @NotNull TipoConexaoImpressora tipoConexao,
        String enderecoIp,
        Integer porta,
        String caminhoCompartilhamento,
        Long armazemId,
        Long depositanteId) {
}