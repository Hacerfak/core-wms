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
-- 1. Cadastrar o Agente (A chave deve ser IGUAL a do seu .env)
INSERT INTO tb_agente_impressao (
        id,
        nome,
        descricao,
        hostname,
        api_key,
        ativo,
        data_criacao
    )
VALUES (
        1,
        'HUB_TESTE',
        'Meu Notebook de Teste',
        'LOCALHOST',
        'chave_secreta_do_teste',
        true,
        NOW()
    ) ON CONFLICT (api_key) DO NOTHING;
-- 2. Cadastrar uma Impressora "Virtual" (Apontando para seu próprio PC)
INSERT INTO tb_impressora (
        id,
        nome,
        tipo_conexao,
        endereco_ip,
        porta,
        ativo,
        armazem_id,
        data_criacao
    )
VALUES (
        1,
        'ZEBRA_VIRTUAL',
        'REDE',
        '127.0.0.1',
        9100,
        true,
        null,
        NOW()
    );
-- Obs: certifique-se que existe armazem_id=1 ou ajuste para null se não for obrigatório