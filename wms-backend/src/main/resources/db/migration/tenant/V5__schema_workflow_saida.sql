CREATE TABLE tb_onda_separacao (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    codigo VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(30) NOT NULL,
    tipo VARCHAR(30) DEFAULT 'NORMAL',
    data_liberacao TIMESTAMP
);
CREATE TABLE tb_solicitacao_saida (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    codigo_externo VARCHAR(100) UNIQUE NOT NULL,
    cliente_id BIGINT NOT NULL,
    onda_id BIGINT,
    status VARCHAR(30) NOT NULL,
    prioridade INTEGER DEFAULT 0,
    data_limite DATE,
    rota VARCHAR(50),
    sequencia_entrega INTEGER,
    CONSTRAINT fk_sol_saida_cli FOREIGN KEY (cliente_id) REFERENCES tb_parceiro(id),
    CONSTRAINT fk_sol_saida_onda FOREIGN KEY (onda_id) REFERENCES tb_onda_separacao(id)
);
CREATE INDEX idx_sol_saida_rota ON tb_solicitacao_saida(rota, status);
CREATE TABLE tb_item_solicitacao_saida (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade_solicitada NUMERIC(18, 4) NOT NULL,
    quantidade_alocada NUMERIC(18, 4) DEFAULT 0,
    quantidade_cortada NUMERIC(18, 4) DEFAULT 0,
    CONSTRAINT fk_item_sol_saida_pai FOREIGN KEY (solicitacao_id) REFERENCES tb_solicitacao_saida(id),
    CONSTRAINT fk_item_sol_saida_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
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
    -- Stage/Doca
    lote_solicitado VARCHAR(50),
    quantidade_planejada NUMERIC(18, 4) NOT NULL,
    quantidade_executada NUMERIC(18, 4),
    CONSTRAINT fk_pick_onda FOREIGN KEY (onda_id) REFERENCES tb_onda_separacao(id),
    CONSTRAINT fk_pick_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_pick_origem FOREIGN KEY (origem_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_pick_destino FOREIGN KEY (destino_id) REFERENCES tb_localizacao(id)
);
-- PACKING / CONFERÊNCIA
CREATE TABLE tb_volume_expedicao (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    solicitacao_id BIGINT NOT NULL,
    codigo_rastreio VARCHAR(50) NOT NULL UNIQUE,
    tipo_embalagem VARCHAR(30),
    peso_bruto NUMERIC(18, 4),
    fechado BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_volume_solicitacao FOREIGN KEY (solicitacao_id) REFERENCES tb_solicitacao_saida(id)
);
CREATE TABLE tb_item_volume_expedicao (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    volume_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade NUMERIC(18, 4) NOT NULL,
    CONSTRAINT fk_item_vol_pai FOREIGN KEY (volume_id) REFERENCES tb_volume_expedicao(id),
    CONSTRAINT fk_item_vol_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);