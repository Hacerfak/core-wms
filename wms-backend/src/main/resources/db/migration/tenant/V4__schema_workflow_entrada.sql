-- ============================================================================
-- 1. FLUXO DE ENTRADA (INBOUND)
-- ============================================================================
-- Solicitação de Entrada (Substitui o antigo Recebimento)
CREATE TABLE tb_solicitacao_entrada (
    id BIGSERIAL PRIMARY KEY,
    -- BaseEntity
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    -- Solicitacao (Base)
    codigo_externo VARCHAR(50),
    data_limite TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    -- CRIADA, EM_PROCESSAMENTO, CONCLUIDA...
    -- Específico
    num_nota_fiscal VARCHAR(50),
    chave_acesso VARCHAR(44),
    data_emissao TIMESTAMP,
    fornecedor_id BIGINT,
    CONSTRAINT fk_sol_ent_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES tb_parceiro(id),
    CONSTRAINT uk_sol_ent_chave UNIQUE (chave_acesso)
);
-- Itens da Solicitação
CREATE TABLE tb_item_solicitacao_entrada (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    solicitacao_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade_prevista NUMERIC(18, 4) NOT NULL,
    quantidade_conferida NUMERIC(18, 4) NOT NULL DEFAULT 0,
    CONSTRAINT fk_item_sol_ent_pai FOREIGN KEY (solicitacao_id) REFERENCES tb_solicitacao_entrada(id),
    CONSTRAINT fk_item_sol_ent_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
-- Tarefa de Conferência (Operador)
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
    cega BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_tarefa_conf_sol FOREIGN KEY (solicitacao_id) REFERENCES tb_solicitacao_entrada(id)
);
-- ============================================================================
-- 2. FLUXO DE ESTOQUE (LPN & ARMAZENAGEM)
-- ============================================================================
-- LPN (License Plate Number) - O "Pallet"
CREATE TABLE tb_lpn (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    codigo VARCHAR(50) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    -- PALLET, CAIXA
    status VARCHAR(20) NOT NULL,
    -- EM_MONTAGEM, FECHADO, ARMAZENADO
    localizacao_atual_id BIGINT,
    solicitacao_entrada_id BIGINT,
    -- Rastreabilidade da origem
    CONSTRAINT uk_lpn_codigo UNIQUE (codigo),
    CONSTRAINT fk_lpn_localizacao FOREIGN KEY (localizacao_atual_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_lpn_sol_ent FOREIGN KEY (solicitacao_entrada_id) REFERENCES tb_solicitacao_entrada(id)
);
-- Itens da LPN (Conteúdo do Pallet)
CREATE TABLE tb_lpn_item (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    lpn_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    quantidade NUMERIC(18, 4) NOT NULL,
    lote VARCHAR(50),
    data_validade DATE,
    CONSTRAINT fk_lpn_item_pai FOREIGN KEY (lpn_id) REFERENCES tb_lpn(id),
    CONSTRAINT fk_lpn_item_produto FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
-- Tarefa de Armazenagem (Put-away)
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
    CONSTRAINT fk_tarefa_arm_lpn FOREIGN KEY (lpn_id) REFERENCES tb_lpn(id),
    CONSTRAINT fk_tarefa_arm_origem FOREIGN KEY (origem_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_tarefa_arm_dest FOREIGN KEY (destino_sugerido_id) REFERENCES tb_localizacao(id)
);
-- Índices de Performance
CREATE INDEX idx_sol_ent_status ON tb_solicitacao_entrada(status);
CREATE INDEX idx_tarefa_conf_status ON tb_tarefa_conferencia(status);
CREATE INDEX idx_lpn_codigo ON tb_lpn(codigo);
CREATE INDEX idx_lpn_status ON tb_lpn(status);
CREATE INDEX idx_tarefa_arm_status ON tb_tarefa_armazenagem(status);