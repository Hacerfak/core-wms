-- Adiciona colunas de auditoria em todas as tabelas principais
-- Produto
ALTER TABLE tb_produto
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_produto
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
-- Parceiro
ALTER TABLE tb_parceiro
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_parceiro
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
-- Localizacao
ALTER TABLE tb_localizacao
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_localizacao
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
-- Recebimento
-- Nota: Se a tabela ja tinha data_criacao, o comando pode falhar.
-- Use 'IF NOT EXISTS' se estiver usando Postgres 9.6+
ALTER TABLE tb_recebimento
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_recebimento
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
-- Pedido Saida
ALTER TABLE tb_pedido_saida
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_pedido_saida
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
-- Usuario
ALTER TABLE tb_usuario
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_usuario
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;
-- Estoque Saldo (Opcional, mas bom para saber a idade do saldo)
ALTER TABLE tb_estoque_saldo
ADD COLUMN IF NOT EXISTS data_criacao TIMESTAMP DEFAULT NOW();
ALTER TABLE tb_estoque_saldo
ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;