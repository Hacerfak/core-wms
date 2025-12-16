CREATE TABLE tb_estoque_saldo (
    id BIGSERIAL PRIMARY KEY,
    produto_id BIGINT NOT NULL,
    localizacao_id BIGINT NOT NULL,
    lote VARCHAR(50),
    data_validade DATE,
    numero_serie VARCHAR(100),
    quantidade NUMERIC(18, 4) NOT NULL,
    version BIGINT,
    CONSTRAINT fk_estoque_produto FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_estoque_local FOREIGN KEY (localizacao_id) REFERENCES tb_localizacao(id),
    -- No Postgres, NULL é considerado distinto de NULL por padrão em constraints antigas.
    -- Para garantir que (ProdA, LocalB, NULL, NULL) não duplique, usamos essa constraint.
    CONSTRAINT uk_estoque_saldo UNIQUE NULLS NOT DISTINCT (produto_id, localizacao_id, lote, numero_serie)
);
-- Índices para performance
CREATE INDEX idx_estoque_produto ON tb_estoque_saldo(produto_id);
CREATE INDEX idx_estoque_local ON tb_estoque_saldo(localizacao_id);
CREATE INDEX idx_estoque_lote ON tb_estoque_saldo(lote);