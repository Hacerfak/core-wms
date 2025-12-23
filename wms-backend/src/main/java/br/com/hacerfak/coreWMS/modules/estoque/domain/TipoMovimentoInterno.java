package br.com.hacerfak.coreWMS.modules.estoque.domain;

public enum TipoMovimentoInterno {
    RESSUPRIMENTO, // Abastecer picking
    CONSOLIDACAO, // Juntar saldos (housekeeping)
    MANUAL // Solicitado pelo operador
}