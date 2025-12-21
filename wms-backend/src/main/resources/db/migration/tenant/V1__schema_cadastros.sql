-- DADOS DA EMPRESA (Tabela de Configuração Única)
CREATE TABLE tb_empresa_dados (
    id BIGINT PRIMARY KEY,
    razao_social VARCHAR(200) NOT NULL,
    nome_fantasia VARCHAR(255),
    cnpj VARCHAR(20) NOT NULL,
    inscricao_estadual VARCHAR(20),
    inscricao_municipal VARCHAR(20),
    cnae_principal VARCHAR(20),
    regime_tributario VARCHAR(20),
    email VARCHAR(255),
    telefone VARCHAR(20),
    website VARCHAR(255),
    cep VARCHAR(10),
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf VARCHAR(2) NOT NULL DEFAULT 'RS',
    endereco_completo VARCHAR(255),
    logo_url VARCHAR(500),
    certificado_arquivo BYTEA,
    certificado_senha VARCHAR(100),
    nome_certificado VARCHAR(255),
    validade_certificado TIMESTAMP,
    -- Auditoria
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP
);
INSERT INTO tb_empresa_dados (id, razao_social, cnpj, uf, criado_por)
VALUES (
        1,
        'A Configurar...',
        '00000000000000',
        'RS',
        'SISTEMA'
    );
-- CONFIGURAÇÃO DO SISTEMA
CREATE TABLE tb_sistema_config (
    chave VARCHAR(100) PRIMARY KEY,
    valor VARCHAR(255),
    descricao VARCHAR(255),
    tipo VARCHAR(20)
);
INSERT INTO tb_sistema_config (chave, valor, descricao, tipo)
VALUES (
        'SISTEMA_MANUTENCAO',
        'false',
        'Coloca o sistema em modo de manutenção',
        'BOOLEAN'
    ),
    (
        'AUDITORIA_RETENCAO_DIAS',
        '90',
        'Dias para manter logs de auditoria (0 = Eterno)',
        'INTEGER'
    ),
    (
        'RECEBIMENTO_CEGO_OBRIGATORIO',
        'true',
        'Oculta quantidades na conferência de entrada',
        'BOOLEAN'
    ),
    (
        'ESTOQUE_NEGATIVO_PERMITIDO',
        'false',
        'Permite expedir produtos sem saldo sistêmico',
        'BOOLEAN'
    ),
    (
        'IMPRESSAO_AUTOMATICA_ETIQUETA',
        'true',
        'Imprime etiquetas automaticamente após conferência',
        'BOOLEAN'
    );
-- ARMAZÉNS
CREATE TABLE tb_armazem (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    endereco_completo VARCHAR(255),
    ativo BOOLEAN DEFAULT TRUE,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT uk_armazem_codigo UNIQUE (codigo)
);
-- ÁREAS
CREATE TABLE tb_area (
    id BIGSERIAL PRIMARY KEY,
    armazem_id BIGINT NOT NULL,
    codigo VARCHAR(10) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    padrao_recebimento BOOLEAN DEFAULT FALSE,
    padrao_expedicao BOOLEAN DEFAULT FALSE,
    padrao_quarentena BOOLEAN DEFAULT FALSE,
    ativo BOOLEAN DEFAULT TRUE,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_area_armazem FOREIGN KEY (armazem_id) REFERENCES tb_armazem(id),
    CONSTRAINT uk_area_codigo_armazem UNIQUE (armazem_id, codigo)
);
-- LOCALIZAÇÕES
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
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_local_area FOREIGN KEY (area_id) REFERENCES tb_area(id),
    CONSTRAINT uk_local_endereco_completo UNIQUE (endereco_completo)
);
CREATE SEQUENCE tb_localizacao_id_seq START 1000;
ALTER TABLE tb_localizacao
ALTER COLUMN id
SET DEFAULT nextval('tb_localizacao_id_seq');
-- SEEDS MAPA
INSERT INTO tb_armazem (id, codigo, nome, endereco_completo, criado_por)
VALUES (
        1,
        'CD01',
        'Centro de Distribuição Principal',
        'Endereço da Empresa',
        'SISTEMA'
    );
SELECT setval(
        'tb_armazem_id_seq',
        (
            SELECT MAX(id)
            FROM tb_armazem
        )
    );
INSERT INTO tb_area (
        id,
        armazem_id,
        codigo,
        nome,
        tipo,
        padrao_recebimento,
        padrao_expedicao,
        padrao_quarentena,
        criado_por
    )
VALUES (
        1,
        1,
        'DOC',
        'Docas Gerais',
        'DOCA',
        true,
        true,
        false,
        'SISTEMA'
    ),
    (
        2,
        1,
        'SEG',
        'Segregados',
        'SEGREGACAO',
        false,
        false,
        true,
        'SISTEMA'
    ),
    (
        3,
        1,
        'GER',
        'Geral',
        'ARMAZENAGEM',
        false,
        false,
        false,
        'SISTEMA'
    );
SELECT setval(
        'tb_area_id_seq',
        (
            SELECT MAX(id)
            FROM tb_area
        )
    );
INSERT INTO tb_localizacao (
        id,
        area_id,
        codigo,
        endereco_completo,
        tipo,
        virtual,
        ativo,
        criado_por
    )
VALUES (
        1,
        1,
        'REC',
        'CD01DOCREC',
        'DOCA',
        true,
        true,
        'SISTEMA'
    ),
    (
        2,
        1,
        'EXP',
        'CD01DOCEXP',
        'DOCA',
        true,
        true,
        'SISTEMA'
    ),
    (
        3,
        2,
        'AV',
        'CD01SEGAV',
        'AVARIA',
        true,
        true,
        'SISTEMA'
    ),
    (
        4,
        2,
        'PERDA',
        'CD01SEGPERDA',
        'PERDA',
        true,
        true,
        'SISTEMA'
    ),
    (
        5,
        2,
        'QUAR',
        'CD01SEGQUAR',
        'QUARENTENA',
        true,
        true,
        'SISTEMA'
    );
-- PARCEIROS
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
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT uk_parceiro_documento UNIQUE (documento)
);
INSERT INTO tb_parceiro (documento, nome, ie, tipo, criado_por)
VALUES (
        '00000000000000',
        'EMPRESA PADRAO',
        'ISENTO',
        'AMBOS',
        'SISTEMA'
    );
-- PRODUTOS
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
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_produto_parceiro FOREIGN KEY (depositante_id) REFERENCES tb_parceiro(id),
    CONSTRAINT uk_produto_sku_depositante UNIQUE (sku, depositante_id)
);
CREATE INDEX idx_produto_ean ON tb_produto(ean13);