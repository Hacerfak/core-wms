package br.com.hacerfak.coreWMS.modules.operacao.domain;

public enum StatusRecebimento {
    AGUARDANDO, // Nota importada, caminhão na doca
    EM_CONFERENCIA, // Operador bipando
    DIVERGENTE, // Contagem não bateu com a Nota
    EM_QUARENTENA, // Produto em quarentena (ex.: avariado)
    FINALIZADO // Estoque efetivado
}
