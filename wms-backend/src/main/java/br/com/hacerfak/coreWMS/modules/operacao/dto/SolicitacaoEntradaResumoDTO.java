package br.com.hacerfak.coreWMS.modules.operacao.dto;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusSolicitacao;
import java.time.LocalDateTime;

public record SolicitacaoEntradaResumoDTO(
        Long id,
        String codigoExterno, // Antigo numeroNota
        String fornecedor,
        StatusSolicitacao status,
        LocalDateTime dataCriacao,
        LocalDateTime dataEmissao) {
}