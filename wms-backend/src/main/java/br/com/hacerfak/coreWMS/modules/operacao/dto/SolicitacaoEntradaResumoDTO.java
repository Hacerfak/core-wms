package br.com.hacerfak.coreWMS.modules.operacao.dto;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusSolicitacao;
import java.time.LocalDateTime;

public record SolicitacaoEntradaResumoDTO(
                Long id,
                String codigoExterno, // Pode ser usado como ref. do agendamento se populado
                String notaFiscal,
                String chaveAcesso, // Novo
                String fornecedorNome,
                StatusSolicitacao status,
                LocalDateTime dataCriacao,
                LocalDateTime dataEmissao,
                Long docaId, // Novo: Para saber se precisa atribuir
                String docaNome // Novo: Para exibir
) {
}