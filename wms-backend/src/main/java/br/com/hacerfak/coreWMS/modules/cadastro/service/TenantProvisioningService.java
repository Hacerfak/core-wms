package br.com.hacerfak.coreWMS.modules.cadastro.service;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
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
        // 1. Verifica se o banco já existe
        String checkSql = "SELECT count(*) FROM pg_database WHERE datname = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tenantId);

        if (count != null && count > 0) {
            System.out.println("Banco de dados " + tenantId + " já existe. Pulando criação.");
        } else {
            // 2. Cria o banco (CREATE DATABASE não pode rodar em transação, por isso
            // jdbcTemplate puro)
            // Atenção: Strings concatenadas aqui exigem cuidado, mas o tenantId vem do
            // nosso controle interno
            String createSql = "CREATE DATABASE " + tenantId;
            jdbcTemplate.execute(createSql);
            System.out.println("Banco de dados " + tenantId + " criado com sucesso!");
        }

        // 3. Roda as migrações (Flyway) no NOVO banco
        rodarMigracoes(tenantId);
    }

    private void rodarMigracoes(String tenantId) {
        String tenantUrl = masterUrl.replace("wms_master", tenantId);

        System.out.println("Iniciando migração (Flyway) para: " + tenantUrl);

        Flyway flyway = Flyway.configure()
                .dataSource(tenantUrl, username, password)
                .locations("classpath:db/migration") // Usa as mesmas migrations do master? Ou teremos específicas?
                // Geralmente tenants tem tabelas de negócio (Produto, Estoque) e Master tem
                // (Empresa, Usuario).
                // Por simplificação inicial, vamos rodar tudo, mas o ideal é separar pastas:
                // db/migration/tenant
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }
}