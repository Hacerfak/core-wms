-- 1. Habilita LPN no Estoque (Fundamental para o novo fluxo)
ALTER TABLE tb_estoque_saldo
ADD COLUMN lpn VARCHAR(50);
-- Atualiza a Unique Constraint do Estoque para suportar LPN
-- (Removemos a constraint antiga e criamos uma nova que inclui o LPN)
ALTER TABLE tb_estoque_saldo DROP CONSTRAINT uk_estoque_saldo;
-- ATUALIZAÇÃO AQUI: Habilita LPN no Histórico de Movimentação também
ALTER TABLE tb_movimento_estoque
ADD COLUMN lpn VARCHAR(50);
-- Cria novas Unique Indexes para o Estoque considerando LPN
CREATE UNIQUE INDEX uk_estoque_saldo_lpn ON tb_estoque_saldo (lpn)
WHERE lpn IS NOT NULL;
CREATE UNIQUE INDEX uk_estoque_saldo_no_lpn ON tb_estoque_saldo (produto_id, localizacao_id, lote, numero_serie)
WHERE lpn IS NULL;
-- 2. Cria a tabela de VOLUMES DO RECEBIMENTO (Onde guardamos o que o operador contou)
CREATE TABLE tb_volume_recebimento (
    id BIGSERIAL PRIMARY KEY,
    recebimento_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    lpn VARCHAR(50) NOT NULL UNIQUE,
    -- O código de barras gerado (Ex: REC-100-1)
    quantidade_original NUMERIC(18, 4) NOT NULL,
    -- Quanto tem neste pallet
    armazenado BOOLEAN DEFAULT FALSE,
    -- Se já foi bipado para guardar
    local_destino_id BIGINT,
    -- Onde foi guardado (após armazenagem)
    data_criacao TIMESTAMP DEFAULT NOW(),
    usuario_criacao VARCHAR(100),
    CONSTRAINT fk_vol_rec FOREIGN KEY (recebimento_id) REFERENCES tb_recebimento(id),
    CONSTRAINT fk_vol_prod FOREIGN KEY (produto_id) REFERENCES tb_produto(id),
    CONSTRAINT fk_vol_local FOREIGN KEY (local_destino_id) REFERENCES tb_localizacao(id)
);