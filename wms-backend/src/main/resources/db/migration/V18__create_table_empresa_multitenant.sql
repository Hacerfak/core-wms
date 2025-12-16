CREATE TABLE tb_empresa (
    id BIGSERIAL PRIMARY KEY,
    razao_social VARCHAR(200) NOT NULL,
    cnpj VARCHAR(20) NOT NULL UNIQUE,
    tenant_id VARCHAR(50) NOT NULL UNIQUE,
    nome_certificado VARCHAR(255),
    validade_certificado DATE,
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP
);
CREATE TABLE tb_usuario_empresa (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    empresa_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    CONSTRAINT fk_usemp_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_usemp_empresa FOREIGN KEY (empresa_id) REFERENCES tb_empresa(id)
);