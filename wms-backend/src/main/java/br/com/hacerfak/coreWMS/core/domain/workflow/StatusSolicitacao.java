package br.com.hacerfak.coreWMS.core.domain.workflow;

public enum StatusSolicitacao {
    CRIADA, // NFe importada ou Agendamento criado
    AGUARDANDO_EXECUCAO, // Doca atribuída, aguardando início físico
    EM_PROCESSAMENTO, // Operador bipou o primeiro item
    DIVERGENTE, // Finalizada com diferenças (Falta/Sobra/Avaria)
    CONCLUIDA,
    BLOQUEADA, // Finalizada com sucesso (100% batido)
    CANCELADA // Abortada
}