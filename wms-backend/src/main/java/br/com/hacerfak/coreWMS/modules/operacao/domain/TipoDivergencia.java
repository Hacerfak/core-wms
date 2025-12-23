package br.com.hacerfak.coreWMS.modules.operacao.domain;

public enum TipoDivergencia {
    FALTA_FISICA, // Nota diz 10, contei 8
    SOBRA_FISICA, // Nota diz 10, contei 12
    AVARIA_RECEBIMENTO, // Recebi, mas está quebrado
    DIVERGENCIA_LOTE, // Nota diz Lote A, físico é Lote B
    DIVERGENCIA_VALIDADE, // Nota diz vencimento A, físico é vencimento B
    DIVERGENCIA_SERIE // Nota diz Série A, físico é Série B
}