package br.com.hacerfak.coreWMS.modules.operacao.event;

import br.com.hacerfak.coreWMS.modules.operacao.domain.SolicitacaoEntrada;

public record EntradaFinalizadaEvent(
                Long solicitacaoId,
                String tenantId,
                SolicitacaoEntrada solicitacao, // Objeto completo para pegar o fornecedor/cliente
                String notaFiscal, // Número da NF para referência no faturamento
                String usuarioResponsavel) {
}