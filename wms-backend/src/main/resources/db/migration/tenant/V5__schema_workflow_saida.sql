-- Remove tabelas antigas de expedição (Se existirem)
DROP TABLE IF EXISTS tb_nota_fiscal CASCADE;
DROP TABLE IF EXISTS tb_tarefa_separacao CASCADE;
DROP TABLE IF EXISTS tb_item_pedido CASCADE;
DROP TABLE IF EXISTS tb_pedido_saida CASCADE;
-- 1. ONDA DE SEPARAÇÃO (Wave)
CREATE TABLE tb_onda_separacao (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    codigo VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_liberacao TIMESTAMP
);
-- 2. SOLICITAÇÃO DE SAÍDA (Substitui PedidoSaida)
CREATE TABLE tb_solicitacao_saida (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    codigo_externo VARCHAR(50),
    data_limite TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    cliente_id BIGINT NOT NULL,
    prioridade INTEGER DEFAULT 0,
    onda_id BIGINT,
    CONSTRAINT uk_sol_saida_ext UNIQUE (codigo_externo),
    CONSTRAINT fk_sol_saida_cli FOREIGN KEY (cliente_id) REFERENCES tb_parceiro(id),
    CONSTRAINT fk_sol_saida_onda FOREIGN KEY (onda_id) REFERENCES tb_onda_separacao(id)
);
-- 3. ITEM DA SOLICITAÇÃO
CREATE TABLE tb_item_solicitacao_saida (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    solicitacao_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade_solicitada NUMERIC(18, 4) NOT NULL,
    quantidade_alocada NUMERIC(18, 4) DEFAULT 0,
    quantidade_separada NUMERIC(18, 4) DEFAULT 0,
    CONSTRAINT fk_item_sol_saida_pai FOREIGN KEY (solicitacao_id) REFERENCES tb_solicitacao_saida(id),
    CONSTRAINT fk_item_sol_saida_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
-- 4. TAREFA DE SEPARAÇÃO (Picking)
CREATE TABLE tb_tarefa_separacao (
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
    onda_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    origem_id BIGINT NOT NULL,
    destino_id BIGINT,
    lote_solicitado VARCHAR(50),
    quantidade_planejada NUMERIC(18, 4) NOT NULL,
    quantidade_executada NUMERIC(18, 4) DEFAULT 0,
    CONSTRAINT fk_tarefa_sep_onda FOREIGN KEY (onda_id) REFERENCES tb_onda_separacao(id),
    CONSTRAINT fk_tarefa_sep_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_tarefa_sep_origem FOREIGN KEY (origem_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_tarefa_sep_dest FOREIGN KEY (destino_id) REFERENCES tb_localizacao(id)
);
-- Índices
CREATE INDEX idx_sol_saida_status ON tb_solicitacao_saida(status);
CREATE INDEX idx_tarefa_sep_status ON tb_tarefa_separacao(status);
CREATE INDEX idx_onda_status ON tb_onda_separacao(status);