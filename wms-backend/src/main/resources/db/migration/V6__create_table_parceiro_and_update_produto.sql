-- 1. Cria a tabela de Parceiros (Emitentes/Depositantes)
CREATE TABLE tb_parceiro (
    id BIGSERIAL PRIMARY KEY,
    documento VARCHAR(20) NOT NULL,
    -- CPF ou CNPJ (apenas números)
    nome VARCHAR(255) NOT NULL,
    -- Razão Social
    ie VARCHAR(20),
    -- Inscrição Estadual
    data_cadastro TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_parceiro_documento UNIQUE (documento)
);
-- 2. Cria um Parceiro "Padrão" para migrar dados antigos (se existirem no teu banco)
-- Isto é necessário para não dar erro ao criar a coluna NOT NULL abaixo.
INSERT INTO tb_parceiro (documento, nome, ie)
VALUES ('00000000000000', 'EMPRESA PADRAO', 'ISENTO');
-- 3. Adiciona a coluna depositante_id na tabela Produto
ALTER TABLE tb_produto
ADD COLUMN depositante_id BIGINT;
-- 4. Atualiza os produtos existentes para pertencerem ao Parceiro Padrão
UPDATE tb_produto
SET depositante_id = (
        SELECT id
        FROM tb_parceiro
        WHERE documento = '00000000000000'
    )
WHERE depositante_id IS NULL;
-- 5. Aplica a restrição NOT NULL e a Chave Estrangeira (FK)
ALTER TABLE tb_produto
ALTER COLUMN depositante_id
SET NOT NULL;
ALTER TABLE tb_produto
ADD CONSTRAINT fk_produto_parceiro FOREIGN KEY (depositante_id) REFERENCES tb_parceiro(id);
-- 6. MUDANÇA CRÍTICA DE REGRA DE NEGÓCIO
-- Remove a regra antiga: "SKU deve ser único no sistema todo"
ALTER TABLE tb_produto DROP CONSTRAINT uk_produto_sku;
-- Cria a nova regra: "SKU deve ser único para CADA depositante"
-- Ex: A Samsung pode ter o SKU '100' e a LG também pode ter o SKU '100'.
ALTER TABLE tb_produto
ADD CONSTRAINT uk_produto_sku_depositante UNIQUE (sku, depositante_id);