package br.com.hacerfak.coreWMS.modules.inventario.domain;

public enum StatusInventario {
    ABERTO, // Planejamento
    EM_EXECUCAO, // Tarefas geradas
    EM_ANALISE, // Contagens feitas, aguardando aprovação de divergência
    FINALIZADO, // Ajustes realizados
    CANCELADO
}