CREATE TABLE tb_usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    -- Novo campo Obrigatório
    login VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255),
    -- Novo campo
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    -- Auditoria Completa BaseEntity
    criado_por VARCHAR(100),
    atualizado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP
);
-- Insert do Admin atualizado com Nome e Email
INSERT INTO tb_usuario (
        nome,
        login,
        email,
        senha,
        role,
        ativo,
        data_criacao,
        criado_por
    )
VALUES (
        'Administrador Master',
        -- Nome agora é obrigatório
        'master',
        'admin@sistema.com',
        -- Email sugerido
        '$2a$10$9lJ7gK5L0SZjQRP15SbKcummYMhAl2AwH3tCIV5N4VyMgYZMce9Mq',
        -- senha: "123456"
        'ADMIN',
        TRUE,
        NOW(),
        'SISTEMA'
    );