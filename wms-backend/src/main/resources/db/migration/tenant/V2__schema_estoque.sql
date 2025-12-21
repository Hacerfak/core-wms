CREATE TABLE tb_estoque_saldo (
    id BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    localizacao_id BIGINT NOT NULL,
    lpn VARCHAR(50),
    lote VARCHAR(50),
    data_validade DATE,
    numero_serie VARCHAR(100),
    quantidade NUMERIC(18, 4) NOT NULL,
    quantidade_reservada NUMERIC(18, 4) NOT NULL DEFAULT 0,
    version BIGINT,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_estoque_produto FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_estoque_local FOREIGN KEY (localizacao_id) REFERENCES tb_localizacao(id)
);
CREATE INDEX idx_estoque_produto ON tb_estoque_saldo(produto_id);
CREATE INDEX idx_estoque_local ON tb_estoque_saldo(localizacao_id);
CREATE INDEX idx_estoque_lote ON tb_estoque_saldo(lote);
CREATE UNIQUE INDEX uk_estoque_saldo_lpn ON tb_estoque_saldo (lpn)
WHERE lpn IS NOT NULL;
CREATE UNIQUE INDEX uk_estoque_saldo_no_lpn ON tb_estoque_saldo (produto_id, localizacao_id, lote, numero_serie)
WHERE lpn IS NULL;
CREATE TABLE tb_movimento_estoque (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,
    produto_id BIGINT NOT NULL,
    localizacao_id BIGINT NOT NULL,
    quantidade NUMERIC(18, 4) NOT NULL,
    lpn VARCHAR(50),
    lote VARCHAR(50),
    numero_serie VARCHAR(100),
    usuario_responsavel VARCHAR(100),
    observacao VARCHAR(255),
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_mov_produto FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_mov_local FOREIGN KEY (localizacao_id) REFERENCES tb_localizacao(id)
);
CREATE INDEX idx_mov_data ON tb_movimento_estoque(data_criacao);
CREATE INDEX idx_mov_produto ON tb_movimento_estoque(produto_id);