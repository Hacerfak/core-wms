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
-- Insere uma empresa padrão para testes iniciais
INSERT INTO tb_empresa (
        razao_social,
        cnpj,
        tenant_id,
        ativo,
        data_criacao
    )
VALUES (
        'Ambiente de Teste',
        '00000000000191',
        'wms_tenant_demo',
        true,
        NOW()
    );
-- Vincula o admin a ela (se existir)
DO $$ BEGIN IF EXISTS (
    SELECT 1
    FROM tb_usuario
    WHERE login = 'admin'
) THEN
INSERT INTO tb_usuario_empresa (usuario_id, empresa_id, role, data_criacao)
SELECT u.id,
    e.id,
    'ADMIN',
    NOW()
FROM tb_usuario u,
    tb_empresa e
WHERE u.login = 'admin'
    AND e.tenant_id = 'wms_tenant_demo';
END IF;
END $$;