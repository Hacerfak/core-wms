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
-- Insere um registro "em branco" ou padrão que será atualizado pelo Java logo em seguida
INSERT INTO tb_empresa_config (
        id,
        razao_social,
        cnpj,
        recebimento_cego_obrigatorio
    )
VALUES (1, 'Configurando...', '00000000000000', true);