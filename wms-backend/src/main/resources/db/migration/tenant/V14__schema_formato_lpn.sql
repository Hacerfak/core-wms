-- 1. CRIAÇÃO DA TABELA DE FORMATOS
CREATE TABLE tb_formato_lpn (
    id BIGSERIAL PRIMARY KEY,
    -- Identificação
    codigo VARCHAR(20) NOT NULL UNIQUE,
    descricao VARCHAR(100) NOT NULL,
    tipo_base VARCHAR(20) NOT NULL,
    -- PALLET, CAIXA, GAIOLA...
    -- Dimensões (Metros)
    altura_m NUMERIC(10, 4),
    largura_m NUMERIC(10, 4),
    profundidade_m NUMERIC(10, 4),
    -- Pesos (Kg)
    peso_suportado_kg NUMERIC(10, 3),
    tara_kg NUMERIC(10, 3),
    -- Controle
    ativo BOOLEAN DEFAULT TRUE,
    -- Auditoria (BaseEntity)
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100)
);
-- 2. ALTERAÇÃO NA TABELA LPN (Vincular ao novo formato)
ALTER TABLE tb_lpn
ADD COLUMN formato_lpn_id BIGINT;
-- Adiciona a constraint (Foreign Key)
ALTER TABLE tb_lpn
ADD CONSTRAINT fk_lpn_formato FOREIGN KEY (formato_lpn_id) REFERENCES tb_formato_lpn(id);
-- Opcional: Criar índice para performance
CREATE INDEX idx_lpn_formato ON tb_lpn(formato_lpn_id);
-- 1. PALLETS
INSERT INTO tb_formato_lpn (
        codigo,
        descricao,
        tipo_base,
        altura_m,
        largura_m,
        profundidade_m,
        peso_suportado_kg,
        tara_kg,
        ativo,
        data_criacao,
        criado_por
    )
VALUES (
        'PBR',
        'Pallet Padrão Brasileiro',
        'PALLET',
        0.15,
        1.00,
        1.20,
        1200.000,
        25.000,
        true,
        NOW(),
        'SISTEMA'
    ),
    (
        'EURO',
        'Pallet Europeu (EPAL)',
        'PALLET',
        0.144,
        0.80,
        1.20,
        1500.000,
        22.000,
        true,
        NOW(),
        'SISTEMA'
    ),
    (
        'CHEP',
        'Pallet CHEP (Azul)',
        'PALLET',
        0.15,
        1.00,
        1.20,
        1500.000,
        28.000,
        true,
        NOW(),
        'SISTEMA'
    );
-- 2. CAIXAS
INSERT INTO tb_formato_lpn (
        codigo,
        descricao,
        tipo_base,
        altura_m,
        largura_m,
        profundidade_m,
        peso_suportado_kg,
        tara_kg,
        ativo,
        data_criacao,
        criado_por
    )
VALUES (
        'CX-P',
        'Caixa Pequena (Correios 1)',
        'CAIXA',
        0.10,
        0.20,
        0.30,
        5.000,
        0.200,
        true,
        NOW(),
        'SISTEMA'
    ),
    (
        'CX-M',
        'Caixa Média Padrão',
        'CAIXA',
        0.30,
        0.40,
        0.50,
        20.000,
        0.500,
        true,
        NOW(),
        'SISTEMA'
    ),
    (
        'CX-G',
        'Caixa Grande Reforçada',
        'CAIXA',
        0.50,
        0.60,
        0.80,
        40.000,
        1.200,
        true,
        NOW(),
        'SISTEMA'
    );
-- 3. OUTROS SUPORTES
INSERT INTO tb_formato_lpn (
        codigo,
        descricao,
        tipo_base,
        altura_m,
        largura_m,
        profundidade_m,
        peso_suportado_kg,
        tara_kg,
        ativo,
        data_criacao,
        criado_por
    )
VALUES (
        'GAIOLA-PADRAO',
        'Gaiola Metálica Aramada',
        'GAIOLA',
        1.00,
        1.00,
        1.20,
        800.000,
        45.000,
        true,
        NOW(),
        'SISTEMA'
    ),
    (
        'TAMBOR-200L',
        'Tambor Plástico 200L',
        'TAMBOR',
        0.90,
        0.60,
        0.60,
        250.000,
        10.000,
        true,
        NOW(),
        'SISTEMA'
    );