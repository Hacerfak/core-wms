package br.com.hacerfak.coreWMS.core.domain.workflow;

public enum StatusTarefa {
    PENDENTE, // Estado genérico inicial
    EM_EXECUCAO, // Em execução (Bipando)
    CONCLUIDA, // Finalizada
    BLOQUEADA, // Travada por algum motivo
    CANCELADA // Cancelada
}