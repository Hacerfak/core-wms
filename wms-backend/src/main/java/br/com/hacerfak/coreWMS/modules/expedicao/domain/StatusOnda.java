package br.com.hacerfak.coreWMS.modules.expedicao.domain;

public enum StatusOnda {
    CRIADA, // Planejamento
    ALOCADA, // Estoque reservado
    EM_SEPARACAO, // Operadores trabalhando
    CONCLUIDA, // Tudo separado
    CANCELADA
}