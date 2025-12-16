-- 1. Ampliando o cadastro de PARCEIRO (Endereço e Fantasia)
ALTER TABLE tb_parceiro
ADD COLUMN nome_fantasia VARCHAR(255);
ALTER TABLE tb_parceiro
ADD COLUMN cep VARCHAR(10);
ALTER TABLE tb_parceiro
ADD COLUMN logradouro VARCHAR(255);
ALTER TABLE tb_parceiro
ADD COLUMN numero VARCHAR(20);
ALTER TABLE tb_parceiro
ADD COLUMN bairro VARCHAR(100);
ALTER TABLE tb_parceiro
ADD COLUMN cidade VARCHAR(100);
ALTER TABLE tb_parceiro
ADD COLUMN uf VARCHAR(2);
ALTER TABLE tb_parceiro
ADD COLUMN telefone VARCHAR(20);
-- 2. Ampliando o cadastro de PRODUTO (Fiscal e Financeiro)
ALTER TABLE tb_produto
ADD COLUMN cest VARCHAR(10);
-- Código Especificador da Substituição Tributária
ALTER TABLE tb_produto
ADD COLUMN valor_unitario_padrao NUMERIC(18, 4);
-- Último valor pago (para base de custo/seguro)