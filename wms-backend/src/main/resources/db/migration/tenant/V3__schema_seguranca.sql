CREATE TABLE tb_perfil (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(100)
);
-- Assumindo que Usuário é uma entidade do tenant (ou replicada)
-- Se for tabela global, não crie aqui. Assumindo modelo isolado:
CREATE TABLE tb_usuario_perfil (
    usuario_id BIGINT NOT NULL,
    perfil_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, perfil_id),
    CONSTRAINT fk_up_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id) -- FK usuario depende de onde está a tabela usuario (se master ou tenant)
);
-- Permissões granulares
CREATE TABLE tb_permissao (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    descricao VARCHAR(255)
);
CREATE TABLE tb_perfil_permissao (
    perfil_id BIGINT NOT NULL,
    permissao_id BIGINT NOT NULL,
    PRIMARY KEY (perfil_id, permissao_id),
    CONSTRAINT fk_pp_perfil FOREIGN KEY (perfil_id) REFERENCES tb_perfil(id),
    CONSTRAINT fk_pp_perm FOREIGN KEY (permissao_id) REFERENCES tb_permissao(id)
);