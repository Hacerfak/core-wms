package br.com.hacerfak.coreWMS.modules.estoque.domain;

public enum TipoMovimento {
    ENTRADA,
    SAIDA,
    AJUSTE_POSITIVO, // Inventário (Sobrou)
    AJUSTE_NEGATIVO, // Inventário (Faltou)
    BLOQUEIO, // Move de "Disponível" para "Avaria" (Logicamente é uma troca)
    DESBLOQUEIO // Move de "Avaria" para "Disponível" (Logicamente é uma troca)
}
