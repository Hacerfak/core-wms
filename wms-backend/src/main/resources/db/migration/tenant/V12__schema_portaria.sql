-- 1. Criação da Tabela de Turnos
CREATE TABLE tb_turno (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    atualizado_por VARCHAR(100),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    nome VARCHAR(50) NOT NULL,
    -- Ex: "Manhã", "Tarde", "Noite"
    inicio TIME NOT NULL,
    fim TIME NOT NULL,
    dias_semana VARCHAR(50),
    -- "SEG,TER,QUA,QUI,SEX"
    ativo BOOLEAN DEFAULT TRUE
);
-- 2. Ajuste na Tabela de Agendamento (Evolução)
-- Estamos "enriquecendo" a tabela existente. Se quiser ser purista, poderia renomear para tb_portaria_agendamento
ALTER TABLE tb_agendamento
ADD COLUMN tipo VARCHAR(20) DEFAULT 'ENTRADA';
-- ENTRADA ou SAIDA
ALTER TABLE tb_agendamento
ADD COLUMN turno_id BIGINT;
ALTER TABLE tb_agendamento
ADD COLUMN solicitacao_saida_id BIGINT;
ALTER TABLE tb_agendamento
ADD COLUMN xml_vinculado BOOLEAN DEFAULT FALSE;
ALTER TABLE tb_agendamento
ADD CONSTRAINT fk_agendamento_turno FOREIGN KEY (turno_id) REFERENCES tb_turno(id);
ALTER TABLE tb_agendamento
ADD CONSTRAINT fk_agendamento_sol_saida FOREIGN KEY (solicitacao_saida_id) REFERENCES tb_solicitacao_saida(id);
-- Índices para performance
CREATE INDEX idx_agendamento_data ON tb_agendamento(data_prevista_inicio);
CREATE INDEX idx_agendamento_status ON tb_agendamento(status);