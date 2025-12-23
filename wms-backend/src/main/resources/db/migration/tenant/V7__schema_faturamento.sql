-- 1. Ajuste no Produto: Vinculo com Depositante
DO $$ BEGIN IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'tb_produto'
        AND column_name = 'depositante_id'
) THEN
ALTER TABLE tb_produto
ADD COLUMN depositante_id BIGINT;
ALTER TABLE tb_produto
ADD CONSTRAINT fk_prod_depositante FOREIGN KEY (depositante_id) REFERENCES tb_parceiro(id);
CREATE INDEX idx_prod_depositante ON tb_produto(depositante_id);
END IF;
END $$;
-- 2. Catálogo de Serviços
CREATE TABLE tb_servico (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nome VARCHAR(100) NOT NULL,
    unidade_medida VARCHAR(20) NOT NULL,
    tipo_cobranca VARCHAR(30) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE
);
-- 3. Acordo de Preço (Contrato)
CREATE TABLE tb_acordo_preco (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    cliente_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    preco_unitario NUMERIC(18, 4) NOT NULL,
    vigencia_inicio DATE NOT NULL,
    vigencia_fim DATE,
    CONSTRAINT fk_acordo_cli FOREIGN KEY (cliente_id) REFERENCES tb_parceiro(id),
    CONSTRAINT fk_acordo_serv FOREIGN KEY (servico_id) REFERENCES tb_servico(id),
    CONSTRAINT uk_acordo_cli_serv UNIQUE (cliente_id, servico_id)
);
-- 4. Fato Gerador (Extrato)
CREATE TABLE tb_apontamento_servico (
    id BIGSERIAL PRIMARY KEY,
    data_evento TIMESTAMP DEFAULT NOW(),
    data_referencia DATE NOT NULL,
    cliente_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    quantidade NUMERIC(18, 4) NOT NULL,
    valor_unitario NUMERIC(18, 4) NOT NULL,
    valor_total NUMERIC(18, 4) NOT NULL,
    origem_referencia VARCHAR(100),
    usuario_apontamento VARCHAR(100),
    observacao VARCHAR(255),
    faturado BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_apont_cli FOREIGN KEY (cliente_id) REFERENCES tb_parceiro(id),
    CONSTRAINT fk_apont_serv FOREIGN KEY (servico_id) REFERENCES tb_servico(id)
);
CREATE INDEX idx_apont_data ON tb_apontamento_servico(data_referencia);
CREATE INDEX idx_apont_cli ON tb_apontamento_servico(cliente_id);