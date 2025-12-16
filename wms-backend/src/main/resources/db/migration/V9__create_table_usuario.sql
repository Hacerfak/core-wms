CREATE TABLE tb_usuario (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    -- Senha Hash (BCrypt)
    role VARCHAR(20) NOT NULL,
    -- ADMIN, OPERADOR, GERENTE
    ativo BOOLEAN DEFAULT TRUE
);
-- Senha padrão '123456' encriptada com BCrypt
INSERT INTO tb_usuario (login, senha, role)
VALUES (
        'admin',
        '$2a$10$9lJ7gK5L0SZjQRP15SbKcummYMhAl2AwH3tCIV5N4VyMgYZMce9Mq',
        'ADMIN'
    );