CREATE TABLE tb_localizacao (
    id BIGINT PRIMARY KEY,
    -- Mudei de BIGSERIAL para BIGINT para controlar manualmente
    codigo VARCHAR(20) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    capacidade_peso_kg NUMERIC(19, 4),
    CONSTRAINT uk_localizacao_codigo UNIQUE (codigo)
);
-- Sequência manual para futuros locais que o usuário criar (começa do 1000 para não bater com os fixos)
CREATE SEQUENCE tb_localizacao_id_seq START 1000;
ALTER TABLE tb_localizacao
ALTER COLUMN id
SET DEFAULT nextval('tb_localizacao_id_seq');
-- INSERTS COM IDs FIXOS (Isso garante que o Java funcione)
INSERT INTO tb_localizacao (id, codigo, tipo, ativo)
VALUES (1, 'RECEBIMENTO', 'DOCA', true),
    -- ID 1 (Usado no RecebimentoService)
    (2, 'EXPEDICAO', 'DOCA', true),
    -- ID 2 (Usado no PickingService)
    (3, 'AVARIA', 'AVARIA', true),
    -- ID 3
    (4, 'PERDA', 'PERDA', true),
    -- ID 4
    (5, 'QUARENTENA', 'QUARENTENA', true);
-- Locais de teste (Pode deixar o banco gerar o ID, ou fixar se quiser)
INSERT INTO tb_localizacao (id, codigo, tipo, ativo)
VALUES (100, 'SEPARACAO', 'PICKING', true),
    (200, 'ARMAZENAGEM', 'PULMAO', true);