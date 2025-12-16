package br.com.hacerfak.coreWMS.modules.expedicao.domain;

public enum StatusPedido {
    CRIADO, // Chegou da API
    ALOCADO, // O sistema já reservou o estoque (gerou tarefas)
    EM_SEPARACAO, // Operador está trabalhando
    SEPARADO, // Tudo na doca
    DESPACHADO // Caminhão saiu (Gera NFe)
}
