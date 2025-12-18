-- 1. CABEÇALHO DO RECEBIMENTO
CREATE TABLE tb_recebimento (
    id BIGSERIAL PRIMARY KEY,
    num_nota_fiscal VARCHAR(50),
    chave_acesso VARCHAR(44),
    fornecedor VARCHAR(200),
    -- Nome histórico do XML
    parceiro_id BIGINT,
    -- Vínculo real
    status VARCHAR(20) NOT NULL,
    data_emissao TIMESTAMP,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_recebimento_parceiro FOREIGN KEY (parceiro_id) REFERENCES tb_parceiro(id)
);
-- 2. ITENS DO RECEBIMENTO (Esperado vs Conferido)
CREATE TABLE tb_item_recebimento (
    id BIGSERIAL PRIMARY KEY,
    recebimento_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade_nota NUMERIC(18, 4) NOT NULL,
    quantidade_conferida NUMERIC(18, 4) DEFAULT 0,
    lote_conferido VARCHAR(50),
    CONSTRAINT fk_item_receb FOREIGN KEY (recebimento_id) REFERENCES tb_recebimento(id),
    CONSTRAINT fk_item_prod_rec FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
-- 3. VOLUMES / ETIQUETAS (LPNs Gerados)
CREATE TABLE tb_volume_recebimento (
    id BIGSERIAL PRIMARY KEY,
    recebimento_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    lpn VARCHAR(50) NOT NULL UNIQUE,
    quantidade_original NUMERIC(18, 4) NOT NULL,
    armazenado BOOLEAN DEFAULT FALSE,
    local_destino_id BIGINT,
    data_criacao TIMESTAMP DEFAULT NOW(),
    usuario_criacao VARCHAR(100),
    CONSTRAINT fk_vol_rec FOREIGN KEY (recebimento_id) REFERENCES tb_recebimento(id),
    CONSTRAINT fk_vol_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_vol_local FOREIGN KEY (local_destino_id) REFERENCES tb_localizacao(id)
);