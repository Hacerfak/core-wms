-- 1. CONFIGURAÇÃO DA EMPRESA (Dados Cadastrais - Singleton)
CREATE TABLE tb_empresa_config (
    id BIGINT PRIMARY KEY,
    razao_social VARCHAR(200) NOT NULL,
    cnpj VARCHAR(20) NOT NULL,
    endereco_completo VARCHAR(255),
    logo_url VARCHAR(500),
    -- Estas colunas abaixo podem ser depreciadas futuramente em favor da tb_configuracao ou tb_parceiro,
    -- mas mantemos por compatibilidade com o código legado do Onboarding.
    permite_estoque_negativo BOOLEAN DEFAULT FALSE,
    recebimento_cego_obrigatorio BOOLEAN DEFAULT TRUE
);
-- Insere o registo padrão (ID 1)
INSERT INTO tb_empresa_config (
        id,
        razao_social,
        cnpj,
        recebimento_cego_obrigatorio
    )
VALUES (1, 'A Configurar...', '00000000000000', true);
-- 1.1 CONFIGURAÇÃO DE PARÂMETROS (Chave-Valor Genérica) - [ADICIONADO PARA CORRIGIR O ERRO]
CREATE TABLE tb_configuracao (
    chave VARCHAR(100) PRIMARY KEY,
    valor VARCHAR(255),
    descricao VARCHAR(255)
);
-- Exemplo de parâmetro global que ainda faz sentido na empresa (Ex: Integração ERP)
INSERT INTO tb_configuracao (chave, valor, descricao)
VALUES (
        'SISTEMA_MANUTENCAO',
        'false',
        'Coloca o sistema em modo de manutenção'
    );
-- 2. LOCALIZAÇÃO (Necessário para Stocks e Movimentos)
CREATE TABLE tb_localizacao (
    id BIGINT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    capacidade_peso_kg NUMERIC(19, 4),
    -- Auditoria
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT uk_localizacao_codigo UNIQUE (codigo)
);
CREATE SEQUENCE tb_localizacao_id_seq START 1000;
ALTER TABLE tb_localizacao
ALTER COLUMN id
SET DEFAULT nextval('tb_localizacao_id_seq');
-- Locais de Sistema (IDs fixos)
INSERT INTO tb_localizacao (id, codigo, tipo, ativo)
VALUES (1, 'RECEBIMENTO', 'DOCA', true),
    (2, 'EXPEDICAO', 'DOCA', true),
    (3, 'AVARIA', 'AVARIA', true),
    (4, 'PERDA', 'PERDA', true),
    (5, 'QUARENTENA', 'QUARENTENA', true);
-- 3. PARCEIROS (Clientes e Fornecedores)
CREATE TABLE tb_parceiro (
    id BIGSERIAL PRIMARY KEY,
    -- Identificação
    documento VARCHAR(20) NOT NULL,
    -- CPF/CNPJ
    nome VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255),
    ie VARCHAR(20),
    crt VARCHAR(5),
    -- Código Regime Tributário
    tipo VARCHAR(20) DEFAULT 'AMBOS',
    -- FORNECEDOR, CLIENTE, AMBOS
    -- Configurações e Status
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    recebimento_cego BOOLEAN NOT NULL DEFAULT FALSE,
    padrao_controla_lote BOOLEAN DEFAULT FALSE,
    padrao_controla_validade BOOLEAN DEFAULT FALSE,
    padrao_controla_serie BOOLEAN DEFAULT FALSE,
    -- Endereço e Contacto
    cep VARCHAR(10),
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf VARCHAR(2),
    telefone VARCHAR(20),
    email VARCHAR(255),
    -- Auditoria
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT uk_parceiro_documento UNIQUE (documento)
);
-- Parceiro Padrão para migrações legadas ou produtos sem dono
INSERT INTO tb_parceiro (documento, nome, ie, tipo)
VALUES (
        '00000000000000',
        'EMPRESA PADRAO',
        'ISENTO',
        'AMBOS'
    );
-- 4. PRODUTOS
CREATE TABLE tb_produto (
    id BIGSERIAL PRIMARY KEY,
    -- Vínculo
    depositante_id BIGINT NOT NULL,
    -- Dados Básicos
    sku VARCHAR(50) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    ean13 VARCHAR(13),
    dun14 VARCHAR(14),
    unidade_medida VARCHAR(5) NOT NULL,
    unidade_armazenagem VARCHAR(10),
    fator_conversao INTEGER DEFAULT 1,
    -- Dados Logísticos e Fiscais
    peso_bruto_kg NUMERIC(10, 3) DEFAULT 0,
    ncm VARCHAR(8),
    cest VARCHAR(10),
    valor_unitario_padrao NUMERIC(18, 4),
    -- Regras
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    controla_lote BOOLEAN DEFAULT FALSE,
    controla_validade BOOLEAN DEFAULT FALSE,
    controla_serie BOOLEAN DEFAULT FALSE,
    -- Auditoria
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_produto_parceiro FOREIGN KEY (depositante_id) REFERENCES tb_parceiro(id),
    CONSTRAINT uk_produto_sku_depositante UNIQUE (sku, depositante_id)
);
CREATE INDEX idx_produto_ean ON tb_produto(ean13);