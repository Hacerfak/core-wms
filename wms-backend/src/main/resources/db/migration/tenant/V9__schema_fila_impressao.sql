-- Cadastro de Impressoras Físicas ou Filas de Rede
CREATE TABLE tb_impressora (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    atualizado_por VARCHAR(100),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    nome VARCHAR(100) NOT NULL UNIQUE,
    -- Ex: "ZEBRA_EXPEDICAO_01"
    descricao VARCHAR(255),
    tipo_conexao VARCHAR(30) NOT NULL,
    -- REDE (IP), USB, COMPARTILHAMENTO
    endereco_ip VARCHAR(50),
    -- 192.168.1.200
    porta INTEGER DEFAULT 9100,
    -- Padrão Zebra
    caminho_compartilhamento VARCHAR(255),
    -- \\SERVIDOR\Zebra01 (Para USB compartilhada)
    ativo BOOLEAN DEFAULT TRUE,
    armazem_id BIGINT,
    -- Para saber onde ela está fisicamente
    depositante_id BIGINT,
    -- Opcional: Se for impressora dedicada de um cliente
    CONSTRAINT fk_imp_armazem FOREIGN KEY (armazem_id) REFERENCES tb_armazem(id),
    CONSTRAINT fk_imp_depositante FOREIGN KEY (depositante_id) REFERENCES tb_parceiro(id)
);
-- A Fila de Tarefas (Jobs)
CREATE TABLE tb_fila_impressao (
    id BIGSERIAL PRIMARY KEY,
    data_criacao TIMESTAMP DEFAULT NOW(),
    data_atualizacao TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    -- PENDENTE, EM_PROCESSAMENTO, CONCLUIDO, ERRO
    zpl_conteudo TEXT NOT NULL,
    impressora_alvo_id BIGINT NOT NULL,
    tentativas INTEGER DEFAULT 0,
    mensagem_erro VARCHAR(500),
    usuario_solicitante VARCHAR(100),
    origem VARCHAR(100),
    -- Ex: "Conferência Mobile", "Expedição Web"
    CONSTRAINT fk_fila_imp FOREIGN KEY (impressora_alvo_id) REFERENCES tb_impressora(id)
);
CREATE INDEX idx_fila_status ON tb_fila_impressao(status, impressora_alvo_id);