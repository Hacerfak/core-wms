CREATE TABLE tb_agente_impressao (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    atualizado_por VARCHAR(100),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    nome VARCHAR(100) NOT NULL UNIQUE,
    -- Ex: "HUB_EXPEDICAO_01"
    descricao VARCHAR(255),
    hostname VARCHAR(100),
    -- Nome da máquina na rede (informativo)
    api_key VARCHAR(100) NOT NULL UNIQUE,
    -- A chave secreta (wms_sk_...)
    ativo BOOLEAN DEFAULT TRUE,
    ultimo_heartbeat TIMESTAMP,
    -- Quando foi a última vez que ele chamou a API
    versao_agente VARCHAR(20) -- Qual versão do .exe ele está rodando
);
CREATE INDEX idx_agente_key ON tb_agente_impressao(api_key);