package br.com.hacerfak.coreWMS.modules.seguranca.domain;

public enum PermissaoEnum {
    // --- CADASTROS BÁSICOS ---
    // Produto
    PRODUTO_VISUALIZAR,
    PRODUTO_CRIAR,
    PRODUTO_EDITAR,
    PRODUTO_EXCLUIR, // Cuidado: geralmente inativação

    // Parceiro (Cliente/Fornecedor)
    PARCEIRO_VISUALIZAR,
    PARCEIRO_CRIAR,
    PARCEIRO_EDITAR,
    PARCEIRO_EXCLUIR,

    // Localização (Ruas, Prédios, Docas)
    LOCALIZACAO_VISUALIZAR,
    LOCALIZACAO_GERENCIAR, // Criar/Editar/Bloquear

    // --- OPERAÇÃO: RECEBIMENTO (Inbound) ---
    RECEBIMENTO_VISUALIZAR, // Ver lista e detalhes
    RECEBIMENTO_IMPORTAR_XML, // Subir XML
    RECEBIMENTO_CONFERIR, // Bipar produtos (App Coletor)
    RECEBIMENTO_FINALIZAR, // Fechar a nota e gerar estoque
    RECEBIMENTO_CANCELAR, // Estornar tudo

    // --- OPERAÇÃO: ESTOQUE (Inventory) ---
    ESTOQUE_VISUALIZAR, // Ver saldos e extratos
    ESTOQUE_MOVIMENTAR, // Ajustes manuais, trocas de local
    ESTOQUE_ARMAZENAR, // Guardar LPN (Putaway)

    // --- OPERAÇÃO: EXPEDIÇÃO (Outbound) ---
    PEDIDO_VISUALIZAR, // Ver pedidos de saída
    PEDIDO_CRIAR, // Criar pedido manual
    PEDIDO_ALOCAR, // Rodar algoritmo de reserva (FEFO)
    PEDIDO_CANCELAR, // Cancelar pedido
    EXPEDICAO_SEPARAR, // Realizar Picking (Coletor)
    EXPEDICAO_DESPACHAR, // Dar baixa final (Caminhão saiu)

    // --- GESTÃO DE ACESSO E CONFIGURAÇÃO ---
    USUARIO_LISTAR,
    USUARIO_CRIAR, // Vincular usuário à empresa
    USUARIO_EXCLUIR, // Remover acesso de usuário na empresa
    PERFIL_GERENCIAR, // Criar/Editar Perfis de Acesso
    CONFIG_GERENCIAR // Alterar configurações da empresa (ex: logo, regras)
}