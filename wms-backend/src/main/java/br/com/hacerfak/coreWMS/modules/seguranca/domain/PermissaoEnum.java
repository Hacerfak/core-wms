package br.com.hacerfak.coreWMS.modules.seguranca.domain;

public enum PermissaoEnum {
    // --- CADASTROS BÁSICOS ---
    PRODUTO_VISUALIZAR,
    PRODUTO_CRIAR,
    PRODUTO_EDITAR,
    PRODUTO_EXCLUIR,

    PARCEIRO_VISUALIZAR,
    PARCEIRO_CRIAR,
    PARCEIRO_EDITAR,
    PARCEIRO_EXCLUIR,

    LOCALIZACAO_VISUALIZAR,
    LOCALIZACAO_GERENCIAR,
    LOCALIZACAO_EXCLUIR,

    // --- OPERAÇÃO: RECEBIMENTO (Inbound) ---
    RECEBIMENTO_VISUALIZAR,
    RECEBIMENTO_IMPORTAR_XML,
    RECEBIMENTO_CONFERIR,
    RECEBIMENTO_FINALIZAR,
    RECEBIMENTO_CANCELAR,

    // --- OPERAÇÃO: ESTOQUE (Inventory) ---
    ESTOQUE_VISUALIZAR,
    ESTOQUE_MOVIMENTAR,
    ESTOQUE_ARMAZENAR,

    // --- OPERAÇÃO: EXPEDIÇÃO (Outbound) ---
    PEDIDO_VISUALIZAR,
    PEDIDO_CRIAR,
    PEDIDO_ALOCAR,
    PEDIDO_CANCELAR,
    EXPEDICAO_SEPARAR, // Picking
    EXPEDICAO_CONFERIR, // Packing (Conferência de Saída)
    EXPEDICAO_DESPACHAR, // Checkout

    // --- OPERAÇÃO: INVENTÁRIO (Novo) ---
    // Usados no InventarioController
    INVENTARIO_CRIAR, // Criar ordens de inventário
    INVENTARIO_CONTAR, // Executar contagem (App Coletor)
    INVENTARIO_APROVAR, // Finalizar e ajustar estoque (Gerente)

    // --- MÓDULO: FATURAMENTO (Novo) ---
    // Usados no FaturamentoController
    FATURAMENTO_VISUALIZAR, // Ver extratos
    FATURAMENTO_APONTAR, // Lançar serviços manuais

    // --- MÓDULO: IMPRESSÃO & SISTEMA ---
    // Usado no AgenteImpressaoController e Configs
    CONFIG_GERENCIAR, // Configurações gerais (Logo, Regras)
    CONFIG_SISTEMA, // Gestão de Agentes de Impressão e Hardware (Renomeado para bater com seu
                    // Controller)

    // --- GESTÃO DE ACESSO ---
    USUARIO_LISTAR,
    USUARIO_CRIAR,
    USUARIO_EDITAR,
    USUARIO_EXCLUIR,
    PERFIL_GERENCIAR,

    // --- AUDITORIA ---
    AUDITORIA_VISUALIZAR
}