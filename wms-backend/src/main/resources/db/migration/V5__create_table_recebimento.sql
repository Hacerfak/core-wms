CREATE TABLE tb_recebimento (
    id BIGSERIAL PRIMARY KEY,
    num_nota_fiscal VARCHAR(50),
    chave_acesso VARCHAR(44),
    fornecedor VARCHAR(200),
    status VARCHAR(20) NOT NULL,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_finalizacao TIMESTAMP
);
CREATE TABLE tb_item_recebimento (
    id BIGSERIAL PRIMARY KEY,
    recebimento_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade_nota NUMERIC(18, 4) NOT NULL,
    quantidade_conferida NUMERIC(18, 4) DEFAULT 0,
    lote_conferido VARCHAR(50),
    CONSTRAINT fk_item_receb FOREIGN KEY (recebimento_id) REFERENCES tb_recebimento(id),
    CONSTRAINT fk_item_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);