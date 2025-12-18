-- 1. PEDIDO DE SAÍDA
CREATE TABLE tb_pedido_saida (
    id BIGSERIAL PRIMARY KEY,
    codigo_pedido_externo VARCHAR(50) UNIQUE,
    cliente_id BIGINT NOT NULL,
    status VARCHAR(20),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    -- Data do Despacho
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES tb_parceiro(id)
);
-- 2. ITENS DO PEDIDO
CREATE TABLE tb_item_pedido (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade_solicitada NUMERIC(18, 4),
    quantidade_alocada NUMERIC(18, 4) DEFAULT 0,
    quantidade_separada NUMERIC(18, 4) DEFAULT 0,
    CONSTRAINT fk_item_ped FOREIGN KEY (pedido_id) REFERENCES tb_pedido_saida(id),
    CONSTRAINT fk_item_prod_ped FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
-- 3. TAREFAS DE SEPARAÇÃO (Picking)
CREATE TABLE tb_tarefa_separacao (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    localizacao_origem_id BIGINT NOT NULL,
    lote_alocado VARCHAR(50),
    quantidade_planejada NUMERIC(18, 4),
    concluida BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_tarefa_ped FOREIGN KEY (pedido_id) REFERENCES tb_pedido_saida(id),
    CONSTRAINT fk_tarefa_loc FOREIGN KEY (localizacao_origem_id) REFERENCES tb_localizacao(id)
);
-- 4. NOTA FISCAL DE SAÍDA
CREATE TABLE tb_nota_fiscal (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    chave_acesso VARCHAR(44),
    numero INTEGER,
    serie INTEGER,
    status VARCHAR(20) NOT NULL,
    xml_assinado TEXT,
    xml_protocolo TEXT,
    motivo_rejeicao VARCHAR(255),
    data_emissao TIMESTAMP,
    CONSTRAINT uk_nfe_pedido UNIQUE (pedido_id),
    CONSTRAINT fk_nfe_pedido FOREIGN KEY (pedido_id) REFERENCES tb_pedido_saida(id)
);