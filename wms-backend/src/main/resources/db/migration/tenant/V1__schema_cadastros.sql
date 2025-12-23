CREATE TABLE tb_parceiro (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    nome VARCHAR(150) NOT NULL,
    cpf_cnpj VARCHAR(20) NOT NULL UNIQUE,
    tipo VARCHAR(30) NOT NULL,
    -- CLIENTE, FORNECEDOR, TRANSPORTADORA
    endereco_completo VARCHAR(255),
    email VARCHAR(100),
    telefone VARCHAR(20),
    ativo BOOLEAN DEFAULT TRUE
);
CREATE INDEX idx_parceiro_doc ON tb_parceiro(cpf_cnpj);
CREATE INDEX idx_parceiro_tipo ON tb_parceiro(tipo);
CREATE TABLE tb_produto (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    sku VARCHAR(50) NOT NULL UNIQUE,
    -- Código de Barras / SKU
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(255),
    categoria VARCHAR(50),
    preco NUMERIC(18, 4),
    peso_bruto NUMERIC(18, 4),
    fator_empilhamento INTEGER DEFAULT 1,
    -- Quantos pallets podem ser empilhados
    ativo BOOLEAN DEFAULT TRUE
);
CREATE INDEX idx_produto_sku ON tb_produto(sku);
-- Dados específicos da Empresa (Tenant) se necessário
CREATE TABLE tb_empresa_dados (
    id BIGSERIAL PRIMARY KEY,
    razao_social VARCHAR(150),
    cnpj VARCHAR(20),
    inscricao_estadual VARCHAR(20),
    endereco_completo VARCHAR(255)
);