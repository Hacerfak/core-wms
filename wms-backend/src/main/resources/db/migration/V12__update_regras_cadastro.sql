-- 1. Atualiza PARCEIRO (Define os padrões do dono do produto)
ALTER TABLE tb_parceiro
ADD COLUMN padrao_controla_lote BOOLEAN DEFAULT FALSE;
ALTER TABLE tb_parceiro
ADD COLUMN padrao_controla_validade BOOLEAN DEFAULT FALSE;
ALTER TABLE tb_parceiro
ADD COLUMN padrao_controla_serie BOOLEAN DEFAULT FALSE;
-- 2. Atualiza PRODUTO (Define as regras específicas e conversão)
ALTER TABLE tb_produto
ADD COLUMN controla_lote BOOLEAN DEFAULT FALSE;
ALTER TABLE tb_produto
ADD COLUMN controla_validade BOOLEAN DEFAULT FALSE;
ALTER TABLE tb_produto
ADD COLUMN controla_serie BOOLEAN DEFAULT FALSE;
-- Fatores de Conversão (Ex: Unidade 'CX' tem fator 12, ou seja, 12 UNs dentro)
ALTER TABLE tb_produto
ADD COLUMN unidade_armazenagem VARCHAR(10);
-- Ex: CX, PALLET
ALTER TABLE tb_produto
ADD COLUMN fator_conversao INTEGER DEFAULT 1;