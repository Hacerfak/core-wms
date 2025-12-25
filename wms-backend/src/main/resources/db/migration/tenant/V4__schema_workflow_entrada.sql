CREATE TABLE tb_solicitacao_entrada (
    id BIGSERIAL PRIMARY KEY,
    -- Auditoria
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    -- Workflow
    codigo_externo VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    data_limite DATE,
    fornecedor_id BIGINT,
    tipo_recebimento VARCHAR(30),
    nota_fiscal VARCHAR(50),
    chave_acesso VARCHAR(44),
    data_emissao TIMESTAMP,
    CONSTRAINT fk_sol_ent_forn FOREIGN KEY (fornecedor_id) REFERENCES tb_parceiro(id)
);
CREATE INDEX idx_sol_ent_nfe ON tb_solicitacao_entrada(nota_fiscal);
ALTER TABLE tb_lpn
ADD CONSTRAINT fk_lpn_solicitacao FOREIGN KEY (solicitacao_entrada_id) REFERENCES tb_solicitacao_entrada(id);
-- --- TABELA CORRIGIDA ---
CREATE TABLE tb_item_solicitacao_entrada (
    id BIGSERIAL PRIMARY KEY,
    -- Auditoria (BaseEntity) - ADICIONADO
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    solicitacao_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade_prevista NUMERIC(18, 4) NOT NULL,
    quantidade_conferida NUMERIC(18, 4) DEFAULT 0,
    CONSTRAINT fk_item_sol_ent_pai FOREIGN KEY (solicitacao_id) REFERENCES tb_solicitacao_entrada(id),
    CONSTRAINT fk_item_sol_ent_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
-- ------------------------
CREATE TABLE tb_agendamento (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    codigo_reserva VARCHAR(50) UNIQUE,
    transportadora_id BIGINT,
    motorista_id BIGINT,
    doca_id BIGINT,
    solicitacao_entrada_id BIGINT,
    data_prevista_inicio TIMESTAMP,
    data_prevista_fim TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    data_chegada TIMESTAMP,
    data_saida TIMESTAMP,
    placa_veiculo VARCHAR(20),
    nome_motorista_avulso VARCHAR(100),
    cpf_motorista_avulso VARCHAR(20),
    CONSTRAINT fk_agendamento_transp FOREIGN KEY (transportadora_id) REFERENCES tb_parceiro(id),
    CONSTRAINT fk_agendamento_doca FOREIGN KEY (doca_id) REFERENCES tb_localizacao(id)
);
CREATE TABLE tb_tarefa_conferencia (
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
    solicitacao_id BIGINT NOT NULL,
    doca_id BIGINT,
    cega BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_conf_solicitacao FOREIGN KEY (solicitacao_id) REFERENCES tb_solicitacao_entrada(id),
    CONSTRAINT fk_conf_doca FOREIGN KEY (doca_id) REFERENCES tb_localizacao(id)
);
CREATE TABLE tb_tarefa_divergencia (
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
    solicitacao_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    quantidade_divergente NUMERIC(18, 4),
    resolucao VARCHAR(500),
    CONSTRAINT fk_div_solicitacao FOREIGN KEY (solicitacao_id) REFERENCES tb_solicitacao_entrada(id),
    CONSTRAINT fk_div_produto FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
CREATE TABLE tb_tarefa_armazenagem (
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
    lpn_id BIGINT NOT NULL,
    origem_id BIGINT,
    destino_sugerido_id BIGINT,
    solicitacao_entrada_id BIGINT,
    CONSTRAINT fk_arm_lpn FOREIGN KEY (lpn_id) REFERENCES tb_lpn(id),
    CONSTRAINT fk_arm_origem FOREIGN KEY (origem_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_arm_destino FOREIGN KEY (destino_sugerido_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_arm_solicitacao FOREIGN KEY (solicitacao_entrada_id) REFERENCES tb_solicitacao_entrada(id)
);