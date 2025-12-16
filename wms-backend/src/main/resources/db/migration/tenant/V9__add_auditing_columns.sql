-- Adiciona colunas de auditoria em todas as tabelas principais
-- Produto
ALTER TABLE tb_produto
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_produto
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
ALTER TABLE tb_produto
ADD COLUMN IF NOT EXISTS data_finalizacao TIMESTAMP;
-- Parceiro
ALTER TABLE tb_parceiro
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_parceiro
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
ALTER TABLE tb_parceiro
ADD COLUMN IF NOT EXISTS data_finalizacao TIMESTAMP;
-- Localizacao
ALTER TABLE tb_localizacao
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_localizacao
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
ALTER TABLE tb_localizacao
ADD COLUMN IF NOT EXISTS data_finalizacao TIMESTAMP;
-- Recebimento
ALTER TABLE tb_recebimento
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_recebimento
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
ALTER TABLE tb_recebimento
ADD COLUMN IF NOT EXISTS data_finalizacao TIMESTAMP;
-- Pedido Saida
ALTER TABLE tb_pedido_saida
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_pedido_saida
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
ALTER TABLE tb_pedido_saida
ADD COLUMN IF NOT EXISTS data_finalizacao TIMESTAMP;
-- Estoque Saldo (Opcional, mas bom para saber a idade do saldo)
ALTER TABLE tb_estoque_saldo
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_estoque_saldo
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
ALTER TABLE tb_estoque_saldo
ADD COLUMN IF NOT EXISTS data_finalizacao TIMESTAMP;