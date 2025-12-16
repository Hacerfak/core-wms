CREATE TABLE tb_empresa_config (
    id BIGINT PRIMARY KEY,
    -- Singleton: Será sempre ID 1
    razao_social VARCHAR(200) NOT NULL,
    cnpj VARCHAR(20) NOT NULL,
    endereco_completo VARCHAR(255),
    logo_url VARCHAR(500),
    -- Configurações Globais do WMS
    permite_estoque_negativo BOOLEAN DEFAULT FALSE,
    recebimento_cego_obrigatorio BOOLEAN DEFAULT TRUE
);
-- Insere a configuração padrão inicial
INSERT INTO tb_empresa_config (
        id,
        razao_social,
        cnpj,
        endereco_completo,
        recebimento_cego_obrigatorio
    )
VALUES (
        1,
        'MINHA EMPRESA WMS',
        '00.000.000/0001-00',
        'Rua Exemplo, 100',
        TRUE
    );