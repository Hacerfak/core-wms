CREATE TABLE tb_movimento_estoque (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,
    data_movimento TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    produto_id BIGINT NOT NULL,
    localizacao_id BIGINT NOT NULL,
    quantidade NUMERIC(18, 4) NOT NULL,
    lote VARCHAR(50),
    numero_serie VARCHAR(100),
    usuario_responsavel VARCHAR(100),
    observacao VARCHAR(255),
    CONSTRAINT fk_mov_produto FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_mov_local FOREIGN KEY (localizacao_id) REFERENCES tb_localizacao(id)
);
CREATE INDEX idx_mov_data ON tb_movimento_estoque(data_movimento);