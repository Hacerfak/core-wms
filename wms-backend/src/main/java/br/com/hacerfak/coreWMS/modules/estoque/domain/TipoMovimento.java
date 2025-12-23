package br.com.hacerfak.coreWMS.modules.estoque.domain;

public enum TipoMovimento {
    ENTRADA, // Recebimento
    SAIDA, // Expedição
    AJUSTE_POSITIVO, // Correção manual (+)
    AJUSTE_NEGATIVO, // Correção manual (-)
    AJUSTE_INVENTARIO, // Gerado pelo módulo de inventário (Auditoria)
    BLOQUEIO, // Mudança de status
    DESBLOQUEIO,
    PERDA_QUEBRA, // Avaria declarada
    CONSUMO_INTERNO
}