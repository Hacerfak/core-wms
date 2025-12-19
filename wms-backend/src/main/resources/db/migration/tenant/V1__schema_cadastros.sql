-- ==========================================================
-- 1. CONFIGURAÇÃO DA EMPRESA (Singleton)
-- ==========================================================
CREATE TABLE tb_empresa_config (
    id BIGINT PRIMARY KEY,
    -- Dados Básicos
    razao_social VARCHAR(200) NOT NULL,
    nome_fantasia VARCHAR(255),
    cnpj VARCHAR(20) NOT NULL,
    inscricao_estadual VARCHAR(20),
    inscricao_municipal VARCHAR(20),
    cnae_principal VARCHAR(20),
    regime_tributario VARCHAR(20),
    -- (CRT: 1=Simples, 3=Normal)
    -- Contato
    email VARCHAR(255),
    telefone VARCHAR(20),
    website VARCHAR(255),
    -- Endereço Estruturado
    cep VARCHAR(10),
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf VARCHAR(2) NOT NULL DEFAULT 'RS',
    endereco_completo VARCHAR(255),
    -- Mantido para compatibilidade/display
    logo_url VARCHAR(500),
    -- CERTIFICADO DIGITAL
    certificado_arquivo BYTEA,
    -- O arquivo .pfx salvo em bytes
    certificado_senha VARCHAR(100),
    -- A senha do certificado
    nome_certificado VARCHAR(255),
    -- Nome original do arquivo
    validade_certificado TIMESTAMP,
    -- Data de expiração extraída
    -- Configurações de Regra
    permite_estoque_negativo BOOLEAN DEFAULT FALSE,
    recebimento_cego_obrigatorio BOOLEAN DEFAULT TRUE
);
-- Seed Inicial da Empresa (ID 1 Fixo)
INSERT INTO tb_empresa_config (
        id,
        razao_social,
        cnpj,
        uf,
        recebimento_cego_obrigatorio
    )
VALUES (
        1,
        'A Configurar...',
        '00000000000000',
        'RS',
        true
    );
-- ==========================================================
-- 1.1 CONFIGURAÇÃO DE PARÂMETROS (Chave-Valor)
-- ==========================================================
CREATE TABLE tb_configuracao (
    chave VARCHAR(100) PRIMARY KEY,
    valor VARCHAR(255),
    descricao VARCHAR(255)
);
INSERT INTO tb_configuracao (chave, valor, descricao)
VALUES (
        'SISTEMA_MANUTENCAO',
        'false',
        'Coloca o sistema em modo de manutenção'
    ),
    (
        'AUDITORIA_RETENCAO_DIAS',
        '90',
        'Dias para manter logs de auditoria (0 = Eterno)'
    );
-- ==========================================================
-- 2. ARMAZÉNS (Nível 1)
-- ==========================================================
CREATE TABLE tb_armazem (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    endereco_completo VARCHAR(255),
    ativo BOOLEAN DEFAULT TRUE,
    -- Auditoria
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT uk_armazem_codigo UNIQUE (codigo)
);
-- ==========================================================
-- 3. ÁREAS (Nível 2 - Zonas dentro do Armazém)
-- ==========================================================
CREATE TABLE tb_area (
    id BIGSERIAL PRIMARY KEY,
    armazem_id BIGINT NOT NULL,
    codigo VARCHAR(10) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    -- Flags de automação
    padrao_recebimento BOOLEAN DEFAULT FALSE,
    padrao_expedicao BOOLEAN DEFAULT FALSE,
    padrao_quarentena BOOLEAN DEFAULT FALSE,
    ativo BOOLEAN DEFAULT TRUE,
    -- Auditoria
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_area_armazem FOREIGN KEY (armazem_id) REFERENCES tb_armazem(id),
    CONSTRAINT uk_area_codigo_armazem UNIQUE (armazem_id, codigo)
);
-- ==========================================================
-- 4. POSIÇÕES / LOCALIZAÇÕES (Nível 3 - Onde o estoque fica)
-- ==========================================================
CREATE TABLE tb_localizacao (
    id BIGINT PRIMARY KEY,
    area_id BIGINT NOT NULL,
    codigo VARCHAR(20) NOT NULL,
    endereco_completo VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    tipo VARCHAR(20) NOT NULL,
    virtual BOOLEAN DEFAULT FALSE,
    permite_multi_lpn BOOLEAN DEFAULT TRUE,
    capacidade_lpn INTEGER DEFAULT 1,
    capacidade_peso_kg NUMERIC(19, 4),
    bloqueado BOOLEAN DEFAULT FALSE,
    ativo BOOLEAN DEFAULT TRUE,
    -- Auditoria
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_local_area FOREIGN KEY (area_id) REFERENCES tb_area(id),
    CONSTRAINT uk_local_endereco_completo UNIQUE (endereco_completo)
);
-- Sequence personalizada para iniciar endereços do usuário a partir de 1000
CREATE SEQUENCE tb_localizacao_id_seq START 1000;
ALTER TABLE tb_localizacao
ALTER COLUMN id
SET DEFAULT nextval('tb_localizacao_id_seq');
-- ==========================================================
-- SEEDS OBRIGATÓRIOS (DADOS INICIAIS)
-- ==========================================================
-- 1. ARMAZÉM PADRÃO (ID 1)
INSERT INTO tb_armazem (id, codigo, nome, endereco_completo)
VALUES (
        1,
        'CD01',
        'Centro de Distribuição Principal',
        'Endereço da Empresa'
    );
-- ATENÇÃO: Corrige a sequence para o próximo ID ser 2
SELECT setval(
        'tb_armazem_id_seq',
        (
            SELECT MAX(id)
            FROM tb_armazem
        )
    );
-- 2. ÁREAS DE SISTEMA (IDs 1, 2, 3)
INSERT INTO tb_area (
        id,
        armazem_id,
        codigo,
        nome,
        tipo,
        padrao_recebimento,
        padrao_expedicao,
        padrao_quarentena
    )
VALUES (
        1,
        1,
        'DOC',
        'Docas Gerais',
        'DOCA',
        true,
        true,
        false
    ),
    (
        2,
        1,
        'SEG',
        'Segregados',
        'SEGREGACAO',
        false,
        false,
        true
    ),
    (
        3,
        1,
        'GER',
        'Geral',
        'ARMAZENAGEM',
        false,
        false,
        false
    );
-- ATENÇÃO: Corrige a sequence para o próximo ID ser 4
SELECT setval(
        'tb_area_id_seq',
        (
            SELECT MAX(id)
            FROM tb_area
        )
    );
-- 3. LOCALIZAÇÕES DE SISTEMA (IDs 1 a 5 Fixos)
-- Endereços formatados como CD01DOCREC (Sem traços)
INSERT INTO tb_localizacao (
        id,
        area_id,
        codigo,
        endereco_completo,
        tipo,
        virtual,
        ativo
    )
VALUES (1, 1, 'REC', 'CD01DOCREC', 'DOCA', true, true),
    (2, 1, 'EXP', 'CD01DOCEXP', 'DOCA', true, true),
    (3, 2, 'AV', 'CD01SEGAV', 'AVARIA', true, true),
    (
        4,
        2,
        'PERDA',
        'CD01SEGPERDA',
        'PERDA',
        true,
        true
    ),
    (
        5,
        2,
        'QUAR',
        'CD01SEGQUAR',
        'QUARENTENA',
        true,
        true
    );
-- (A sequence de localização já começa em 1000, não precisa de ajuste manual aqui)
-- ==========================================================
-- 5. PARCEIROS
-- ==========================================================
CREATE TABLE tb_parceiro (
    id BIGSERIAL PRIMARY KEY,
    documento VARCHAR(20) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255),
    ie VARCHAR(20),
    crt VARCHAR(5),
    tipo VARCHAR(20) DEFAULT 'AMBOS',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    recebimento_cego BOOLEAN NOT NULL DEFAULT FALSE,
    padrao_controla_lote BOOLEAN DEFAULT FALSE,
    padrao_controla_validade BOOLEAN DEFAULT FALSE,
    padrao_controla_serie BOOLEAN DEFAULT FALSE,
    cep VARCHAR(10),
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf VARCHAR(2),
    telefone VARCHAR(20),
    email VARCHAR(255),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT uk_parceiro_documento UNIQUE (documento)
);
INSERT INTO tb_parceiro (documento, nome, ie, tipo)
VALUES (
        '00000000000000',
        'EMPRESA PADRAO',
        'ISENTO',
        'AMBOS'
    );
-- ==========================================================
-- 6. PRODUTOS
-- ==========================================================
CREATE TABLE tb_produto (
    id BIGSERIAL PRIMARY KEY,
    depositante_id BIGINT NOT NULL,
    sku VARCHAR(50) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    ean13 VARCHAR(13),
    dun14 VARCHAR(14),
    unidade_medida VARCHAR(5) NOT NULL,
    unidade_armazenagem VARCHAR(10),
    fator_conversao INTEGER DEFAULT 1,
    peso_bruto_kg NUMERIC(10, 3) DEFAULT 0,
    ncm VARCHAR(8),
    cest VARCHAR(10),
    valor_unitario_padrao NUMERIC(18, 4),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    controla_lote BOOLEAN DEFAULT FALSE,
    controla_validade BOOLEAN DEFAULT FALSE,
    controla_serie BOOLEAN DEFAULT FALSE,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_produto_parceiro FOREIGN KEY (depositante_id) REFERENCES tb_parceiro(id),
    CONSTRAINT uk_produto_sku_depositante UNIQUE (sku, depositante_id)
);
CREATE INDEX idx_produto_ean ON tb_produto(ean13);