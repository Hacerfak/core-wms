-- 1. PERFIS DE ACESSO
CREATE TABLE tb_perfil (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP
);
-- 2. PERMISSÕES DO PERFIL
CREATE TABLE tb_perfil_permissoes (
    perfil_id BIGINT NOT NULL,
    permissao VARCHAR(50) NOT NULL,
    CONSTRAINT fk_pp_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id)
);
-- 3. VÍNCULO USUÁRIO (GLOBAL) -> PERFIL (LOCAL)
CREATE TABLE tb_usuario_perfil (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    perfil_id BIGINT NOT NULL,
    CONSTRAINT fk_up_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id),
    CONSTRAINT uk_usuario_perfil UNIQUE (usuario_id, perfil_id)
);
-- =================================================================================
-- CARGA INICIAL (SEEDS)
-- =================================================================================
INSERT INTO tb_perfil (nome, descricao)
VALUES (
        'Administrador',
        'Acesso total e irrestrito ao ambiente da empresa.'
    ),
    (
        'Gerente',
        'Gestão de estoque, cadastros e configurações.'
    ),
    (
        'Operador',
        'Execução de Recebimento, Armazenagem e Expedição.'
    );
-- PERMISSÕES: ADMIN
INSERT INTO tb_perfil_permissoes (perfil_id, permissao)
SELECT id,
    unnest(
        ARRAY [
    'PRODUTO_VISUALIZAR', 'PRODUTO_CRIAR', 'PRODUTO_EDITAR', 'PRODUTO_EXCLUIR',
    'PARCEIRO_VISUALIZAR', 'PARCEIRO_CRIAR', 'PARCEIRO_EDITAR', 'PARCEIRO_EXCLUIR',
    'LOCALIZACAO_VISUALIZAR', 'LOCALIZACAO_GERENCIAR',
    'RECEBIMENTO_VISUALIZAR', 'RECEBIMENTO_IMPORTAR_XML', 'RECEBIMENTO_CONFERIR', 'RECEBIMENTO_FINALIZAR', 'RECEBIMENTO_CANCELAR',
    'ESTOQUE_VISUALIZAR', 'ESTOQUE_MOVIMENTAR', 'ESTOQUE_ARMAZENAR',
    'PEDIDO_VISUALIZAR', 'PEDIDO_CRIAR', 'PEDIDO_ALOCAR', 'PEDIDO_CANCELAR',
    'EXPEDICAO_SEPARAR', 'EXPEDICAO_DESPACHAR',
    'USUARIO_LISTAR', 'USUARIO_CRIAR', 'PERFIL_GERENCIAR', 'CONFIG_GERENCIAR'
]
    )
FROM tb_perfil
WHERE nome = 'Administrador';
-- PERMISSÕES: GERENTE
INSERT INTO tb_perfil_permissoes (perfil_id, permissao)
SELECT id,
    unnest(
        ARRAY [
    'PRODUTO_VISUALIZAR', 'PRODUTO_CRIAR', 'PRODUTO_EDITAR', 'PRODUTO_EXCLUIR',
    'PARCEIRO_VISUALIZAR', 'PARCEIRO_CRIAR', 'PARCEIRO_EDITAR', 'PARCEIRO_EXCLUIR',
    'LOCALIZACAO_VISUALIZAR', 'LOCALIZACAO_GERENCIAR',
    'RECEBIMENTO_VISUALIZAR', 'RECEBIMENTO_IMPORTAR_XML', 'RECEBIMENTO_CONFERIR', 'RECEBIMENTO_FINALIZAR', 'RECEBIMENTO_CANCELAR',
    'ESTOQUE_VISUALIZAR', 'ESTOQUE_MOVIMENTAR', 'ESTOQUE_ARMAZENAR',
    'PEDIDO_VISUALIZAR', 'PEDIDO_CRIAR', 'PEDIDO_ALOCAR', 'PEDIDO_CANCELAR',
    'EXPEDICAO_SEPARAR', 'EXPEDICAO_DESPACHAR',
    'CONFIG_GERENCIAR'
]
    )
FROM tb_perfil
WHERE nome = 'Gerente';
-- PERMISSÕES: OPERADOR
INSERT INTO tb_perfil_permissoes (perfil_id, permissao)
SELECT id,
    unnest(
        ARRAY [
    'PRODUTO_VISUALIZAR', 'PARCEIRO_VISUALIZAR', 'LOCALIZACAO_VISUALIZAR',
    'RECEBIMENTO_VISUALIZAR', 'RECEBIMENTO_CONFERIR',
    'ESTOQUE_VISUALIZAR', 'ESTOQUE_ARMAZENAR',
    'PEDIDO_VISUALIZAR', 'EXPEDICAO_SEPARAR', 'EXPEDICAO_DESPACHAR'
]
    )
FROM tb_perfil
WHERE nome = 'Operador';