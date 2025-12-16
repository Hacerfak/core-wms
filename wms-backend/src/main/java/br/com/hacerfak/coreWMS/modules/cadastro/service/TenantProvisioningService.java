package br.com.hacerfak.coreWMS.modules.cadastro.service;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource; // Importante!
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class TenantProvisioningService {

    private final JdbcTemplate jdbcTemplate; // Conectado ao MASTER

    @Value("${spring.datasource.url}")
    private String masterUrl;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;

    public TenantProvisioningService(DataSource masterDataSource) {
        this.jdbcTemplate = new JdbcTemplate(masterDataSource);
    }

    public void criarBancoDeDados(String tenantId) {
        // ... (código de verificação e criação do banco permanece igual) ...
        String checkSql = "SELECT count(*) FROM pg_database WHERE datname = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tenantId);

        if (count == null || count == 0) {
            String createSql = "CREATE DATABASE " + tenantId;
            jdbcTemplate.execute(createSql);
            System.out.println("Banco de dados " + tenantId + " criado com sucesso!");
        }

        // 3. Roda as migrações (Cria as tabelas, incluindo tb_empresa_config)
        rodarMigracoes(tenantId);
    }

    // --- NOVO MÉTODO ---
    public void inicializarConfiguracao(String tenantId, String razaoSocial, String cnpj) {
        System.out.println(">>> Inicializando configurações para: " + tenantId);

        // 1. Cria uma conexão temporária direta com o banco do NOVO cliente
        String tenantUrl = masterUrl.replace("wms_master", tenantId);
        if (tenantUrl.equals(masterUrl)) {
            tenantUrl = "jdbc:postgresql://wms-db-main:5432/" + tenantId;
        }

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(tenantUrl);
        ds.setUsername(username);
        ds.setPassword(password);

        JdbcTemplate tenantJdbc = new JdbcTemplate(ds);

        // 2. Atualiza a tabela de configuração (ID 1) com os dados reais
        String sql = """
                    UPDATE tb_empresa_config
                    SET razao_social = ?,
                        cnpj = ?,
                        recebimento_cego_obrigatorio = ?
                    WHERE id = 1
                """;

        // Aqui definimos os defaults. Por exemplo, recebimento cego começa TRUE.
        tenantJdbc.update(sql, razaoSocial, cnpj, true);

        System.out.println(">>> Configuração do tenant " + tenantId + " atualizada com sucesso!");
    }

    private void rodarMigracoes(String tenantId) {
        // ... (código do flyway mantido igual, apontando para pasta tenant) ...
        String tenantUrl = masterUrl.replace("wms_master", tenantId);
        if (tenantUrl.equals(masterUrl)) {
            tenantUrl = "jdbc:postgresql://wms-db-main:5432/" + tenantId;
        }

        Flyway flyway = Flyway.configure()
                .dataSource(tenantUrl, username, password)
                .locations("classpath:db/migration/tenant") // Garanta que está apontando para a pasta certa
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }
}