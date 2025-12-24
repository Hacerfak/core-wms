CREATE TABLE tb_perfil (
    id BIGSERIAL PRIMARY KEY,
    -- Campos de Auditoria (herdados de BaseEntity)
    criado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    atualizado_por VARCHAR(100),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    -- Campos da Entidade Perfil
    nome VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(100),
    ativo BOOLEAN DEFAULT TRUE
);
-- Tabela para armazenar os Enums de permissão (Mapeamento @ElementCollection)
CREATE TABLE tb_perfil_permissoes (
    perfil_id BIGINT NOT NULL,
    permissao VARCHAR(100) NOT NULL,
    CONSTRAINT fk_pp_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id)
);
-- Tabela de junção Usuario <-> Perfil
-- (Geralmente necessária se a relação for @ManyToMany na classe Usuario)
CREATE TABLE tb_usuario_perfil (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    perfil_id BIGINT NOT NULL,
    -- Campos de Auditoria (herdados de BaseEntity)
    criado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    atualizado_por VARCHAR(100),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT uk_usuario_perfil UNIQUE (usuario_id, perfil_id),
    CONSTRAINT fk_up_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id)
);
-- SEEDS
INSERT INTO tb_perfil (nome, descricao, criado_por)
VALUES (
        'Administrador',
        'Acesso total e irrestrito ao ambiente da empresa.',
        'SISTEMA'
    ),
    (
        'Gerente',
        'Gestão de estoque, cadastros e configurações.',
        'SISTEMA'
    ),
    (
        'Operador',
        'Execução de Recebimento, Armazenagem e Expedição.',
        'SISTEMA'
    );
INSERT INTO tb_perfil_permissoes (perfil_id, permissao)
SELECT id,
    unnest(
        ARRAY [
    'PRODUTO_VISUALIZAR', 'PRODUTO_CRIAR', 'PRODUTO_EDITAR', 'PRODUTO_EXCLUIR',
    'PARCEIRO_VISUALIZAR', 'PARCEIRO_CRIAR', 'PARCEIRO_EDITAR', 'PARCEIRO_EXCLUIR',
    'LOCALIZACAO_VISUALIZAR', 'LOCALIZACAO_GERENCIAR', 'LOCALIZACAO_EXCLUIR',
    'RECEBIMENTO_VISUALIZAR', 'RECEBIMENTO_IMPORTAR_XML', 'RECEBIMENTO_CONFERIR', 'RECEBIMENTO_FINALIZAR', 'RECEBIMENTO_CANCELAR',
    'ESTOQUE_VISUALIZAR', 'ESTOQUE_MOVIMENTAR', 'ESTOQUE_ARMAZENAR',
    'PEDIDO_VISUALIZAR', 'PEDIDO_CRIAR', 'PEDIDO_ALOCAR', 'PEDIDO_CANCELAR',
    'EXPEDICAO_SEPARAR', 'EXPEDICAO_DESPACHAR',
    'USUARIO_LISTAR', 'USUARIO_CRIAR', 'USUARIO_EDITAR', 'USUARIO_EXCLUIR', 'PERFIL_GERENCIAR', 'CONFIG_GERENCIAR', 'AUDITORIA_VISUALIZAR'
]
    )
FROM tb_perfil
WHERE nome = 'Administrador';
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
    'CONFIG_GERENCIAR', 'USUARIO_LISTAR'
]
    )
FROM tb_perfil
WHERE nome = 'Gerente';
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