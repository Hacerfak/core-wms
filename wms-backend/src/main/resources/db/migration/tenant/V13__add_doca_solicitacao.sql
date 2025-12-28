ALTER TABLE tb_solicitacao_entrada
ADD COLUMN doca_id BIGINT;
ALTER TABLE tb_solicitacao_entrada
ADD CONSTRAINT fk_sol_ent_doca FOREIGN KEY (doca_id) REFERENCES tb_localizacao(id);
ALTER TABLE tb_solicitacao_saida
ADD COLUMN doca_id BIGINT;
ALTER TABLE tb_solicitacao_saida
ADD CONSTRAINT fk_solicitacao_saida_doca FOREIGN KEY (doca_id) REFERENCES tb_localizacao(id);