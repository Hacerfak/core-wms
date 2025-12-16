CREATE TABLE tb_nota_fiscal (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    chave_acesso VARCHAR(44),
    numero INTEGER,
    serie INTEGER,
    status VARCHAR(20) NOT NULL,
    xml_assinado TEXT,
    xml_protocolo TEXT,
    motivo_rejeicao VARCHAR(255),
    data_emissao TIMESTAMP,
    CONSTRAINT uk_nfe_pedido UNIQUE (pedido_id),
    CONSTRAINT fk_nfe_pedido FOREIGN KEY (pedido_id) REFERENCES tb_pedido_saida(id)
);