CREATE TABLE tb_etiqueta_template (
    id BIGSERIAL PRIMARY KEY,
    criado_por VARCHAR(100),
    data_criacao TIMESTAMP DEFAULT NOW(),
    atualizado_por VARCHAR(100),
    data_atualizacao TIMESTAMP,
    data_finalizacao TIMESTAMP,
    nome VARCHAR(100) NOT NULL,
    tipo_finalidade VARCHAR(50) NOT NULL,
    -- LPN, PRODUTO, VOLUME_EXPEDICAO, LOCALIZACAO
    zpl_codigo TEXT NOT NULL,
    -- O código ZPL com placeholders {{VARIAVEL}}
    largura_mm INTEGER,
    altura_mm INTEGER,
    padrao BOOLEAN DEFAULT FALSE,
    depositante_id BIGINT,
    CONSTRAINT fk_tmpl_depositante FOREIGN KEY (depositante_id) REFERENCES tb_parceiro(id)
);
CREATE INDEX idx_etiqueta_tipo ON tb_etiqueta_template(tipo_finalidade, padrao);