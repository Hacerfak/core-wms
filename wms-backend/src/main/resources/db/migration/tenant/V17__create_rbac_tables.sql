-- 1. Tabela de Perfis (Ex: Gerente, Operador)
CREATE TABLE tb_perfil (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    ativo BOOLEAN DEFAULT TRUE,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP
);
-- 2. Tabela de Permissões (Lista de Strings do Enum vinculadas ao Perfil)
CREATE TABLE tb_perfil_permissoes (
    perfil_id BIGINT NOT NULL,
    permissao VARCHAR(50) NOT NULL,
    CONSTRAINT fk_pp_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id)
);
-- 3. Tabela de Vínculo: Usuário Global (ID) <-> Perfil Local
CREATE TABLE tb_usuario_perfil (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    -- ID do usuário no banco Master
    perfil_id BIGINT NOT NULL,
    -- ID do perfil neste banco Tenant
    CONSTRAINT fk_up_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id),
    CONSTRAINT uk_usuario_perfil UNIQUE (usuario_id, perfil_id)
);
-- 4. Insere perfis padrão para facilitar
INSERT INTO tb_perfil (nome, descricao)
VALUES ('Administrador Local', 'Acesso total à empresa');
INSERT INTO tb_perfil (nome, descricao)
VALUES (
        'Operador',
        'Acesso operacional (Recebimento e Expedição)'
    );
-- Permissões do Admin Local (Exemplo parcial)
INSERT INTO tb_perfil_permissoes (perfil_id, permissao)
VALUES (
        (
            SELECT id
            FROM tb_perfil
            WHERE nome = 'Administrador Local'
        ),
        'RECEBIMENTO_VISUALIZAR'
    ),
    (
        (
            SELECT id
            FROM tb_perfil
            WHERE nome = 'Administrador Local'
        ),
        'RECEBIMENTO_CONFERIR'
    ),
    (
        (
            SELECT id
            FROM tb_perfil
            WHERE nome = 'Administrador Local'
        ),
        'USUARIO_CRIAR'
    );