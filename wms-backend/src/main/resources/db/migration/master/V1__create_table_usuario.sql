CREATE TABLE tb_usuario (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    -- ADICIONE ESTES CAMPOS OBRIGATÓRIOS DA BASEENTITY:
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP
);
-- Mantém o insert do admin
INSERT INTO tb_usuario (login, senha, role, ativo, data_criacao)
VALUES (
        'master',
        '$2a$10$9lJ7gK5L0SZjQRP15SbKcummYMhAl2AwH3tCIV5N4VyMgYZMce9Mq',
        -- senha: "123456"
        'ADMIN',
        TRUE,
        NOW()
    );