-- Adicionar status_qualidade nas tabelas de estoque e LPN
ALTER TABLE tb_lpn_item
ADD COLUMN status_qualidade VARCHAR(20) DEFAULT 'DISPONIVEL';
ALTER TABLE tb_estoque_saldo
ADD COLUMN status_qualidade VARCHAR(20) DEFAULT 'DISPONIVEL';
-- Tabela de Tarefa de Divergência
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
    -- FALTA, SOBRA, AVARIA
    quantidade_divergente NUMERIC(18, 4),
    resolucao VARCHAR(500),
    CONSTRAINT fk_div_solicitacao FOREIGN KEY (solicitacao_id) REFERENCES tb_solicitacao_entrada(id),
    CONSTRAINT fk_div_produto FOREIGN KEY (produto_id) REFERENCES tb_produto(id)
);
-- Atualizar constraint unique do saldo para considerar qualidade
-- (Se tiver produto igual, lote igual, mas um é BOM e outro RUIM, são linhas diferentes)
ALTER TABLE tb_estoque_saldo DROP CONSTRAINT IF EXISTS uk_estoque_saldo;
ALTER TABLE tb_estoque_saldo DROP CONSTRAINT IF EXISTS uk_estoque_saldo_no_lpn;
CREATE UNIQUE INDEX uk_estoque_saldo_full ON tb_estoque_saldo (
    produto_id,
    localizacao_id,
    lote,
    numero_serie,
    status_qualidade
)
WHERE lpn IS NULL;
ALTER TABLE tb_movimento_estoque
ADD COLUMN saldo_anterior NUMERIC(18, 4);
ALTER TABLE tb_movimento_estoque
ADD COLUMN saldo_atual NUMERIC(18, 4);
-- Índice para busca rápida por LPN no histórico
CREATE INDEX idx_mov_lpn ON tb_movimento_estoque(lpn);
ALTER TABLE tb_lpn_item
ADD COLUMN numero_serie VARCHAR(100);
-- TABELA DE AGENDAMENTO
CREATE TABLE tb_agendamento (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    codigo_reserva VARCHAR(50),
    transportadora_id BIGINT,
    motorista_id BIGINT,
    doca_id BIGINT,
    solicitacao_entrada_id BIGINT,
    data_prevista_inicio TIMESTAMP,
    data_prevista_fim TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    -- Execução
    data_chegada TIMESTAMP,
    data_saida TIMESTAMP,
    placa_veiculo VARCHAR(20),
    nome_motorista_avulso VARCHAR(100),
    cpf_motorista_avulso VARCHAR(20),
    CONSTRAINT uk_agendamento_cod UNIQUE (codigo_reserva),
    CONSTRAINT fk_agendamento_transp FOREIGN KEY (transportadora_id) REFERENCES tb_parceiro(id),
    CONSTRAINT fk_agendamento_doca FOREIGN KEY (doca_id) REFERENCES tb_localizacao(id)
);
-- TABELA DE ANEXOS (Evidências)
CREATE TABLE tb_anexo (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    nome_arquivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    caminho_url VARCHAR(500) NOT NULL,
    entidade_id BIGINT NOT NULL,
    entidade_tipo VARCHAR(50) NOT NULL,
    descricao VARCHAR(255)
);
CREATE INDEX idx_anexo_entidade ON tb_anexo(entidade_id, entidade_tipo);
CREATE TABLE tb_inventario (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    descricao VARCHAR(100),
    tipo VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    data_agendada DATE,
    cego BOOLEAN DEFAULT TRUE,
    max_tentativas INTEGER DEFAULT 1
);
CREATE TABLE tb_tarefa_contagem (
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
    inventario_id BIGINT NOT NULL,
    localizacao_id BIGINT NOT NULL,
    produto_foco_id BIGINT,
    saldo_sistema_snapshot NUMERIC(18, 4),
    quantidade_contada1 NUMERIC(18, 4),
    usuario_contagem1 VARCHAR(100),
    quantidade_contada2 NUMERIC(18, 4),
    usuario_contagem2 VARCHAR(100),
    quantidade_contada3 NUMERIC(18, 4),
    usuario_contagem3 VARCHAR(100),
    quantidade_final NUMERIC(18, 4),
    divergente BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_tarefa_cont_inv FOREIGN KEY (inventario_id) REFERENCES tb_inventario(id),
    CONSTRAINT fk_tarefa_cont_loc FOREIGN KEY (localizacao_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_tarefa_cont_prod FOREIGN KEY (produto_foco_id) REFERENCES tb_produto(id)
);
-- Atualizar Localização
ALTER TABLE tb_localizacao
ADD COLUMN tipo_estrutura VARCHAR(30);
ALTER TABLE tb_localizacao
ADD COLUMN capacidade_maxima INTEGER DEFAULT 1;
-- Atualizar Produto
ALTER TABLE tb_produto
ADD COLUMN fator_empilhamento INTEGER DEFAULT 1;
-- Atualizar dados legados (opcional)
UPDATE tb_localizacao
SET tipo_estrutura = 'BLOCADO',
    capacidade_maxima = 1
WHERE tipo_estrutura IS NULL;
CREATE TABLE tb_configuracao_picking (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    produto_id BIGINT NOT NULL,
    localizacao_id BIGINT NOT NULL,
    ponto_ressuprimento NUMERIC(18, 4) NOT NULL,
    capacidade_maxima NUMERIC(18, 4) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_conf_pick_prod_loc UNIQUE (produto_id, localizacao_id),
    CONSTRAINT fk_conf_pick_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_conf_pick_loc FOREIGN KEY (localizacao_id) REFERENCES tb_localizacao(id)
);
CREATE TABLE tb_tarefa_movimentacao (
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
    tipo_movimento VARCHAR(30) NOT NULL,
    produto_id BIGINT NOT NULL,
    origem_id BIGINT NOT NULL,
    destino_id BIGINT NOT NULL,
    quantidade NUMERIC(18, 4),
    lpn_id BIGINT,
    CONSTRAINT fk_tm_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_tm_origem FOREIGN KEY (origem_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_tm_destino FOREIGN KEY (destino_id) REFERENCES tb_localizacao(id),
    CONSTRAINT fk_tm_lpn FOREIGN KEY (lpn_id) REFERENCES tb_lpn(id)
);
-- Atualizar Solicitação com Roteirização
ALTER TABLE tb_solicitacao_saida
ADD COLUMN rota VARCHAR(50);
ALTER TABLE tb_solicitacao_saida
ADD COLUMN sequencia_entrega INTEGER;
CREATE INDEX idx_sol_saida_rota ON tb_solicitacao_saida(rota);
-- Tabelas de Packing (Conferência)
CREATE TABLE tb_volume_expedicao (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    solicitacao_id BIGINT NOT NULL,
    codigo_rastreio VARCHAR(50) NOT NULL,
    tipo_embalagem VARCHAR(30),
    peso_bruto NUMERIC(18, 4),
    fechado BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_volume_rastreio UNIQUE (codigo_rastreio),
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