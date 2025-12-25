CREATE TABLE tb_sistema_config (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    chave VARCHAR(100) NOT NULL UNIQUE,
    valor VARCHAR(500),
    descricao VARCHAR(255),
    tipo VARCHAR(50) -- Adicionado para corrigir o erro (STRING, BOOLEAN, etc)
);
CREATE TABLE tb_anexo (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    nome_arquivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    caminho_url VARCHAR(500) NOT NULL,
    entidade_id BIGINT NOT NULL,
    entidade_tipo VARCHAR(50) NOT NULL,
    descricao VARCHAR(255)
);
CREATE INDEX idx_anexo_entidade ON tb_anexo(entidade_id, entidade_tipo);
CREATE TABLE tb_inventario (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    descricao VARCHAR(100),
    tipo VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_agendada DATE,
    cego BOOLEAN DEFAULT TRUE,
    max_tentativas INTEGER DEFAULT 1
);
CREATE TABLE tb_tarefa_contagem (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    usuario_atribuido VARCHAR(100),
    inicio_execucao TIMESTAMP,
    fim_execucao TIMESTAMP,
    inventario_id BIGINT NOT NULL,
    localizacao_id BIGINT NOT NULL,
    produto_foco_id BIGINT,
    saldo_sistema_snapshot NUMERIC(18, 4),
    quantidade_contada1 NUMERIC(18, 4),
    usuario_contagem1 VARCHAR(100),
    quantidade_contada2 NUMERIC(18, 4),
    usuario_contagem2 VARCHAR(100),
    quantidade_contada3 NUMERIC(18, 4),
    usuario_contagem3 VARCHAR(100),
    quantidade_final NUMERIC(18, 4),
    divergente BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_tarefa_cont_inv FOREIGN KEY (inventario_id) REFERENCES tb_inventario(id),
    CONSTRAINT fk_tarefa_cont_loc FOREIGN KEY (localizacao_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_tarefa_cont_prod FOREIGN KEY (produto_foco_id) REFERENCES tb_produto(id)
);