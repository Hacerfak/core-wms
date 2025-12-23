CREATE TABLE tb_armazem (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    nome VARCHAR(100) NOT NULL,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    endereco_completo VARCHAR(255)
);
CREATE TABLE tb_area (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    nome VARCHAR(100) NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    armazem_id BIGINT NOT NULL,
    CONSTRAINT fk_area_armazem FOREIGN KEY (armazem_id) REFERENCES tb_armazem(id)
);
CREATE TABLE tb_localizacao (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    codigo VARCHAR(50) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    -- PULMAO, PICKING, DOCA, AVARIA
    tipo_estrutura VARCHAR(30) DEFAULT 'PORTA_PALLET',
    -- BLOCADO, DRIVE_IN
    area_id BIGINT NOT NULL,
    capacidade_maxima INTEGER DEFAULT 1,
    ativo BOOLEAN DEFAULT TRUE,
    bloqueado BOOLEAN DEFAULT FALSE,
    motivo_bloqueio VARCHAR(255),
    CONSTRAINT fk_local_area FOREIGN KEY (area_id) REFERENCES tb_area(id),
    CONSTRAINT uk_local_codigo UNIQUE (codigo, area_id)
);
CREATE INDEX idx_local_codigo ON tb_localizacao(codigo);
CREATE INDEX idx_local_tipo ON tb_localizacao(tipo);
CREATE TABLE tb_lpn (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    tipo VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    -- EM_MONTAGEM, ARMAZENADO, EXPEDIDO
    localizacao_atual_id BIGINT,
    solicitacao_entrada_id BIGINT,
    -- FK adicionada no V4 para evitar erro circular
    CONSTRAINT fk_lpn_local FOREIGN KEY (localizacao_atual_id) REFERENCES tb_localizacao(id)
);
CREATE INDEX idx_lpn_codigo ON tb_lpn(codigo);
CREATE TABLE tb_lpn_item (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    lpn_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade NUMERIC(18, 4) NOT NULL,
    lote VARCHAR(50),
    data_validade DATE,
    numero_serie VARCHAR(100),
    status_qualidade VARCHAR(20) DEFAULT 'DISPONIVEL',
    CONSTRAINT fk_lpn_item_pai FOREIGN KEY (lpn_id) REFERENCES tb_lpn(id),
    CONSTRAINT fk_lpn_item_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
CREATE TABLE tb_estoque_saldo (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT,
    -- Optimistic Locking
    produto_id BIGINT NOT NULL,
    localizacao_id BIGINT NOT NULL,
    lpn VARCHAR(50),
    -- Denormalizado para performance
    lote VARCHAR(50),
    numero_serie VARCHAR(100),
    data_validade DATE,
    status_qualidade VARCHAR(20) DEFAULT 'DISPONIVEL',
    quantidade NUMERIC(18, 4) NOT NULL DEFAULT 0,
    quantidade_reservada NUMERIC(18, 4) NOT NULL DEFAULT 0,
    data_criacao TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_saldo_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_saldo_local FOREIGN KEY (localizacao_id) REFERENCES tb_localizacao(id)
);
-- Índices compostos vitais para performance do FEFO e Picking
CREATE INDEX idx_saldo_busca ON tb_estoque_saldo(produto_id, localizacao_id, status_qualidade);
CREATE INDEX idx_saldo_validade ON tb_estoque_saldo(produto_id, data_validade);
CREATE TABLE tb_configuracao_picking (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    produto_id BIGINT NOT NULL,
    localizacao_id BIGINT NOT NULL,
    ponto_ressuprimento NUMERIC(18, 4) NOT NULL,
    capacidade_maxima NUMERIC(18, 4) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_conf_pick UNIQUE (produto_id, localizacao_id),
    CONSTRAINT fk_conf_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_conf_loc FOREIGN KEY (localizacao_id) REFERENCES tb_localizacao(id)
);
CREATE TABLE tb_movimento_estoque (
    id BIGSERIAL PRIMARY KEY,
    data_criacao TIMESTAMP DEFAULT NOW(),
    tipo VARCHAR(30) NOT NULL,
    produto_id BIGINT NOT NULL,
    localizacao_id BIGINT NOT NULL,
    quantidade NUMERIC(18, 4) NOT NULL,
    saldo_anterior NUMERIC(18, 4),
    saldo_atual NUMERIC(18, 4),
    lpn VARCHAR(50),
    lote VARCHAR(50),
    numero_serie VARCHAR(100),
    usuario_responsavel VARCHAR(100),
    observacao VARCHAR(255)
);
CREATE INDEX idx_mov_prod_loc ON tb_movimento_estoque(produto_id, localizacao_id);
CREATE INDEX idx_mov_data ON tb_movimento_estoque(data_criacao);
-- Tarefas de Movimentação Interna (Ressuprimento, etc)
CREATE TABLE tb_tarefa_movimentacao (
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
    tipo_movimento VARCHAR(30) NOT NULL,
    -- RESSUPRIMENTO, CONSOLIDACAO
    produto_id BIGINT NOT NULL,
    origem_id BIGINT NOT NULL,
    destino_id BIGINT NOT NULL,
    quantidade NUMERIC(18, 4),
    lpn_id BIGINT,
    CONSTRAINT fk_tm_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_tm_origem FOREIGN KEY (origem_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_tm_destino FOREIGN KEY (destino_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_tm_lpn FOREIGN KEY (lpn_id) REFERENCES tb_lpn(id)
);