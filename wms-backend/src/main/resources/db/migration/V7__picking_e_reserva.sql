-- 1. Tabelas de Pedido
CREATE TABLE tb_pedido_saida (
    id BIGSERIAL PRIMARY KEY,
    codigo_pedido_externo VARCHAR(50) UNIQUE,
    cliente_id BIGINT NOT NULL,
    status VARCHAR(20),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_finalizacao TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES tb_parceiro(id)
);
CREATE TABLE tb_item_pedido (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade_solicitada NUMERIC(18, 4),
    quantidade_alocada NUMERIC(18, 4) DEFAULT 0,
    quantidade_separada NUMERIC(18, 4) DEFAULT 0,
    CONSTRAINT fk_item_ped FOREIGN KEY (pedido_id) REFERENCES tb_pedido_saida(id),
    CONSTRAINT fk_item_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
-- 2. Tabela de Tarefas
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
-- 3. CRÍTICO: Adiciona coluna de Reserva no Saldo
ALTER TABLE tb_estoque_saldo
ADD COLUMN quantidade_reservada NUMERIC(18, 4) NOT NULL DEFAULT 0;