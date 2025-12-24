-- TABELA PARCEIRO (Clientes, Fornecedores, Transportadoras)
CREATE TABLE tb_parceiro (
    id BIGSERIAL PRIMARY KEY,
    -- Auditoria BaseEntity
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    -- Dados Principais
    nome VARCHAR(150) NOT NULL,
    cpf_cnpj VARCHAR(20) NOT NULL UNIQUE,
    tipo VARCHAR(30) NOT NULL,
    -- CLIENTE, FORNECEDOR, TRANSPORTADORA
    nome_fantasia VARCHAR(150),
    ie VARCHAR(20),
    crt VARCHAR(5),
    -- Endereço Detalhado (CORREÇÃO DO ERRO 'column bairro does not exist')
    cep VARCHAR(10),
    logradouro VARCHAR(255),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf VARCHAR(2),
    -- Contato
    email VARCHAR(100),
    telefone VARCHAR(20),
    -- Configurações
    ativo BOOLEAN DEFAULT TRUE,
    recebimento_cego BOOLEAN DEFAULT FALSE,
    padrao_controla_lote BOOLEAN DEFAULT FALSE,
    padrao_controla_validade BOOLEAN DEFAULT FALSE,
    padrao_controla_serie BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_parceiro_doc ON tb_parceiro(cpf_cnpj);
CREATE INDEX idx_parceiro_tipo ON tb_parceiro(tipo);
-- TABELA PRODUTO
CREATE TABLE tb_produto (
    id BIGSERIAL PRIMARY KEY,
    -- Auditoria
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    -- Identificação
    sku VARCHAR(50) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(255),
    -- Caso exista na classe pai ou futuro
    -- Vínculo Obrigatório (CORREÇÃO: Faltava no script anterior)
    depositante_id BIGINT NOT NULL,
    -- Códigos e Fiscal
    ean13 VARCHAR(13),
    dun14 VARCHAR(14),
    ncm VARCHAR(10),
    cest VARCHAR(10),
    -- Unidades e Dimensões
    unidade_medida VARCHAR(10),
    peso_bruto_kg NUMERIC(10, 3) DEFAULT 0,
    valor_unitario_padrao NUMERIC(18, 4),
    -- Configurações Logísticas
    ativo BOOLEAN DEFAULT TRUE,
    controla_lote BOOLEAN DEFAULT FALSE,
    controla_validade BOOLEAN DEFAULT FALSE,
    controla_serie BOOLEAN DEFAULT FALSE,
    -- Conversão
    unidade_armazenagem VARCHAR(10),
    fator_conversao INTEGER DEFAULT 1,
    fator_empilhamento INTEGER DEFAULT 1,
    -- Constraints
    CONSTRAINT fk_produto_depositante FOREIGN KEY (depositante_id) REFERENCES tb_parceiro(id),
    CONSTRAINT uk_produto_sku_depositante UNIQUE (sku, depositante_id)
);
CREATE INDEX idx_produto_sku ON tb_produto(sku);
CREATE INDEX idx_produto_ean ON tb_produto(ean13);
-- TABELA EMPRESA DADOS (Configurações do Tenant)
CREATE TABLE tb_empresa_dados (
    id BIGSERIAL PRIMARY KEY,
    -- Auditoria
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    -- Dados
    razao_social VARCHAR(150),
    nome_fantasia VARCHAR(150),
    cnpj VARCHAR(20),
    inscricao_estadual VARCHAR(20),
    inscricao_municipal VARCHAR(20),
    cnae_principal VARCHAR(20),
    regime_tributario VARCHAR(50),
    email VARCHAR(100),
    telefone VARCHAR(20),
    website VARCHAR(100),
    -- Endereço
    cep VARCHAR(10),
    logradouro VARCHAR(150),
    numero VARCHAR(20),
    complemento VARCHAR(100),
    bairro VARCHAR(100),
    cidade VARCHAR(100),
    uf VARCHAR(2),
    -- Certificado
    certificado_arquivo BYTEA,
    certificado_senha VARCHAR(255),
    nome_certificado VARCHAR(100),
    validade_certificado TIMESTAMP
);
-- Inicializa registro único
INSERT INTO tb_empresa_dados (id, razao_social, data_criacao, criado_por)
VALUES (1, 'Configuração Pendente', NOW(), 'SISTEMA') ON CONFLICT (id) DO NOTHING;