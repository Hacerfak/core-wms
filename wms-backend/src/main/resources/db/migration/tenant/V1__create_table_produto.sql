CREATE TABLE tb_produto (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    ean13 VARCHAR(13),
    unidade_medida VARCHAR(5) NOT NULL,
    peso_bruto_kg NUMERIC(10, 3),
    ncm VARCHAR(8),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    data_atualizacao TIMESTAMP WITHOUT TIME ZONE,
    data_finalizacao TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_produto_sku UNIQUE (sku)
);
-- Índices para performance nas consultas do coletor de dados
CREATE INDEX idx_produto_ean ON tb_produto(ean13);