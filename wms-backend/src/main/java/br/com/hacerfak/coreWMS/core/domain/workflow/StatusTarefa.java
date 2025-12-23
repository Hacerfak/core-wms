package br.com.hacerfak.coreWMS.core.domain.workflow;

public enum StatusTarefa {
    PENDENTE, // Tarefa criada, aguardando início (ex: na fila do coletor)
    EM_EXECUCAO, // Operador assumiu a tarefa e está trabalhando nela
    CONCLUIDA, // Trabalho finalizado com sucesso
    BLOQUEADA, // Algo impede a execução (ex: endereço bloqueado, saldo insuficiente na hora H)
    CANCELADA // Tarefa abortada manualmente ou pelo sistema
}