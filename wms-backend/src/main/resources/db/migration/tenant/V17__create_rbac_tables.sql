-- 1. Tabela de Perfis (Ex: Gerente, Operador)
CREATE TABLE tb_perfil (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP
);
-- 2. Tabela de Permissões (Lista de Strings do Enum vinculadas ao Perfil)
CREATE TABLE tb_perfil_permissoes (
    perfil_id BIGINT NOT NULL,
    permissao VARCHAR(50) NOT NULL,
    CONSTRAINT fk_pp_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id)
);
-- 3. Tabela de Vínculo: Usuário Global (ID) <-> Perfil Local
CREATE TABLE tb_usuario_perfil (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    -- ID do usuário no banco Master
    perfil_id BIGINT NOT NULL,
    -- ID do perfil neste banco Tenant
    CONSTRAINT fk_up_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id),
    CONSTRAINT uk_usuario_perfil UNIQUE (usuario_id, perfil_id)
);
-- =================================================================================
-- CARGA INICIAL DE PERFIS PADRÃO
-- =================================================================================
-- 1. ADMINISTRADOR LOCAL (Acesso total dentro da empresa)
INSERT INTO tb_perfil (nome, descricao)
VALUES (
        'Administrador',
        'Acesso total e irrestrito ao ambiente da empresa.'
    );
-- 2. GERENTE (Gestão da operação e cadastros, sem acesso a usuários/perfis)
INSERT INTO tb_perfil (nome, descricao)
VALUES (
        'Gerente',
        'Gestão de estoque, cadastros e configurações. Sem acesso a usuários.'
    );
-- 3. OPERADOR (Apenas execução de tarefas, sem exclusão ou edição de cadastros)
INSERT INTO tb_perfil (nome, descricao)
VALUES (
        'Operador',
        'Execução de Recebimento, Armazenagem e Expedição.'
    );
-- =================================================================================
-- CARGA DE PERMISSÕES
-- =================================================================================
-- --- A. PERMISSÕES DO ADMINISTRADOR (TUDO) ---
INSERT INTO tb_perfil_permissoes (perfil_id, permissao)
SELECT id,
    unnest(
        ARRAY [
    -- Cadastros
    'PRODUTO_VISUALIZAR', 'PRODUTO_CRIAR', 'PRODUTO_EDITAR', 'PRODUTO_EXCLUIR',
    'PARCEIRO_VISUALIZAR', 'PARCEIRO_CRIAR', 'PARCEIRO_EDITAR', 'PARCEIRO_EXCLUIR',
    'LOCALIZACAO_VISUALIZAR', 'LOCALIZACAO_GERENCIAR',
    -- Recebimento
    'RECEBIMENTO_VISUALIZAR', 'RECEBIMENTO_IMPORTAR_XML', 'RECEBIMENTO_CONFERIR', 'RECEBIMENTO_FINALIZAR', 'RECEBIMENTO_CANCELAR',
    -- Estoque
    'ESTOQUE_VISUALIZAR', 'ESTOQUE_MOVIMENTAR', 'ESTOQUE_ARMAZENAR',
    -- Expedição
    'PEDIDO_VISUALIZAR', 'PEDIDO_CRIAR', 'PEDIDO_ALOCAR', 'PEDIDO_CANCELAR',
    'EXPEDICAO_SEPARAR', 'EXPEDICAO_DESPACHAR',
    -- Gestão (Exclusivo do Admin)
    'USUARIO_LISTAR', 'USUARIO_CRIAR',
    'PERFIL_GERENCIAR',
    'CONFIG_GERENCIAR'
]
    )
FROM tb_perfil
WHERE nome = 'Administrador';
-- --- B. PERMISSÕES DO GERENTE (TUDO MENOS USUÁRIOS/PERFIS) ---
INSERT INTO tb_perfil_permissoes (perfil_id, permissao)
SELECT id,
    unnest(
        ARRAY [
    -- Cadastros (Pode gerenciar produtos e parceiros)
    'PRODUTO_VISUALIZAR', 'PRODUTO_CRIAR', 'PRODUTO_EDITAR', 'PRODUTO_EXCLUIR',
    'PARCEIRO_VISUALIZAR', 'PARCEIRO_CRIAR', 'PARCEIRO_EDITAR', 'PARCEIRO_EXCLUIR',
    'LOCALIZACAO_VISUALIZAR', 'LOCALIZACAO_GERENCIAR',
    -- Recebimento (Pode importar e cancelar erros dos operadores)
    'RECEBIMENTO_VISUALIZAR', 'RECEBIMENTO_IMPORTAR_XML', 'RECEBIMENTO_CONFERIR', 'RECEBIMENTO_FINALIZAR', 'RECEBIMENTO_CANCELAR',
    -- Estoque
    'ESTOQUE_VISUALIZAR', 'ESTOQUE_MOVIMENTAR', 'ESTOQUE_ARMAZENAR',
    -- Expedição
    'PEDIDO_VISUALIZAR', 'PEDIDO_CRIAR', 'PEDIDO_ALOCAR', 'PEDIDO_CANCELAR',
    'EXPEDICAO_SEPARAR', 'EXPEDICAO_DESPACHAR',
    -- Configuração (Pode mudar regras da empresa)
    'CONFIG_GERENCIAR'
    -- NOTA: Não tem USUARIO_* nem PERFIL_*
]
    )
FROM tb_perfil
WHERE nome = 'Gerente';
-- --- C. PERMISSÕES DO OPERADOR (APENAS EXECUÇÃO E VISUALIZAÇÃO) ---
INSERT INTO tb_perfil_permissoes (perfil_id, permissao)
SELECT id,
    unnest(
        ARRAY [
    -- Cadastros (Apenas vê para consultar dúvidas)
    'PRODUTO_VISUALIZAR',
    'PARCEIRO_VISUALIZAR',
    'LOCALIZACAO_VISUALIZAR',
    -- Recebimento (Apenas confere o que chegou)
    'RECEBIMENTO_VISUALIZAR', 'RECEBIMENTO_CONFERIR',
    -- Estoque (Pode guardar e consultar, mas não ajusta saldo na mão sem auditoria)
    'ESTOQUE_VISUALIZAR', 'ESTOQUE_ARMAZENAR',
    -- Expedição (Separa e carrega caminhão)
    'PEDIDO_VISUALIZAR',
    'EXPEDICAO_SEPARAR', 'EXPEDICAO_DESPACHAR'
    -- NOTA: Não tem DELETE, CANCELAR, CRIAR, nem CONFIG
]
    )
FROM tb_perfil
WHERE nome = 'Operador';