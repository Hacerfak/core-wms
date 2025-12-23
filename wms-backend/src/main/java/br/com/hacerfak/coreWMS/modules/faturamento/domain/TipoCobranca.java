package br.com.hacerfak.coreWMS.modules.faturamento.domain;

public enum TipoCobranca {
    ARMAZENAGEM, // Cobrado por saldo diário (Job)
    OPERACIONAL_AUTO, // Cobrado por evento do sistema (Picking, Recebimento)
    OPERACIONAL_MANUAL, // Cobrado via apontamento no coletor (Etiquetagem, Retrabalho)
    FIXO_MENSAL // Valor fixo (Minimo, Aluguel de Sala)
}