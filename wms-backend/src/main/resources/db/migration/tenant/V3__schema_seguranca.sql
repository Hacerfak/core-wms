CREATE TABLE tb_perfil (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    ativo BOOLEAN DEFAULT TRUE,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP
);
CREATE TABLE tb_perfil_permissoes (
    perfil_id BIGINT NOT NULL,
    permissao VARCHAR(50) NOT NULL,
    CONSTRAINT fk_pp_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id)
);
CREATE TABLE tb_usuario_perfil (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    perfil_id BIGINT NOT NULL,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_up_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id),
    CONSTRAINT uk_usuario_perfil UNIQUE (usuario_id, perfil_id)
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