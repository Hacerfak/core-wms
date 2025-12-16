CREATE TABLE tb_configuracao (
    chave VARCHAR(100) PRIMARY KEY,
    valor VARCHAR(255),
    descricao VARCHAR(255)
);
-- Insere a configuração padrão (TRUE = Mostra a quantidade, FALSE = Cega total)
INSERT INTO tb_configuracao (chave, valor, descricao)
VALUES (
        'RECEBIMENTO_EXIBIR_QTD_ESPERADA',
        'true',
        'Exibir quantidade da nota na conferência'
    );