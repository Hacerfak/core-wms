package br.com.hacerfak.coreWMS.modules.seguranca.domain;

public enum PermissaoEnum {
    // Operação
    RECEBIMENTO_VISUALIZAR,
    RECEBIMENTO_IMPORTAR_XML,
    RECEBIMENTO_CONFERIR,
    ESTOQUE_MOVIMENTAR,

    // Cadastros
    PRODUTO_CRIAR,
    PARCEIRO_CRIAR,

    // Gestão
    USUARIO_CRIAR,
    USUARIO_LISTAR,
    PERFIL_GERENCIAR
}