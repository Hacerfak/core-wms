package br.com.hacerfak.coreWMS.modules.impressao.domain;

public enum StatusImpressao {
    PENDENTE, // Aguardando Agente pegar
    EM_PROCESSAMENTO, // Agente pegou e está enviando
    CONCLUIDO, // Sucesso
    ERRO, // Falha após N tentativas
    CANCELADO
}