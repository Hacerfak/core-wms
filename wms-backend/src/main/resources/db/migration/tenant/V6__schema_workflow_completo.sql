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