package br.com.hacerfak.coreWMS.modules.cadastro.service;

import br.com.hacerfak.coreWMS.core.config.MultiTenantConfig;
import br.com.hacerfak.coreWMS.core.multitenant.MultiTenantDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class TenantProvisioningService {

    private final JdbcTemplate jdbcTemplate;
    private final MultiTenantDataSource multiTenantDataSource;
    private final MultiTenantConfig multiTenantConfig;

    @Value("${spring.datasource.url}")
    private String masterUrl;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;

    public TenantProvisioningService(DataSource masterDataSource,
            @Lazy MultiTenantDataSource multiTenantDataSource,
            MultiTenantConfig multiTenantConfig) {
        this.jdbcTemplate = new JdbcTemplate(masterDataSource);
        this.multiTenantDataSource = multiTenantDataSource;
        this.multiTenantConfig = multiTenantConfig;
    }

    public void criarBancoDeDados(String tenantId) {
        String checkSql = "SELECT count(*) FROM pg_database WHERE datname = ?";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tenantId);

        if (count == null || count == 0) {
            String createSql = "CREATE DATABASE " + tenantId;
            jdbcTemplate.execute(createSql);
            System.out.println("Banco de dados " + tenantId + " criado com sucesso!");
        }
        rodarMigracoes(tenantId);
    }

    private void registrarTenantNoPool(String tenantId) {
        DataSource novoDs = multiTenantConfig.createTenantDataSource(tenantId);
        multiTenantDataSource.addTenant(tenantId, novoDs);
    }

    public void inicializarConfiguracao(String tenantId, String razaoSocial, String cnpj) {
        System.out.println(">>> Inicializando dados e configs para: " + tenantId);

        String tenantUrl = masterUrl.replace("wms_master", tenantId);
        if (tenantUrl.equals(masterUrl)) {
            tenantUrl = "jdbc:postgresql://wms-db-main:5432/" + tenantId;
        }

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(tenantUrl);
        ds.setUsername(username);
        ds.setPassword(password);

        JdbcTemplate tenantJdbc = new JdbcTemplate(ds);

        // 1. Inicializa TB_EMPRESA_DADOS (ID 1)
        // O Flyway já deve ter criado a tabela com uma linha vazia ou criamos agora
        // Assumindo que a migration V1 insere um registro vazio com ID 1:
        String sqlEmpresa = """
                    UPDATE tb_empresa_dados
                    SET razao_social = ?, cnpj = ?
                    WHERE id = 1
                """;
        // Se a migration não inserir o ID 1, mude para INSERT.
        // Vou assumir UPDATE pois é o padrão que você tinha.
        int rows = tenantJdbc.update(sqlEmpresa, razaoSocial, cnpj);
        if (rows == 0) {
            tenantJdbc.update("INSERT INTO tb_empresa_dados (id, razao_social, cnpj) VALUES (1, ?, ?)", razaoSocial,
                    cnpj);
        }

        // 2. Inicializa TB_SISTEMA_CONFIG (Configurações Padrão)
        // Aqui definimos os defaults do sistema
        inserirConfigSeNaoExistir(tenantJdbc, "RECEBIMENTO_CEGO_OBRIGATORIO", "true",
                "Oculta quantidades na conferência", "BOOLEAN");
        inserirConfigSeNaoExistir(tenantJdbc, "ESTOQUE_NEGATIVO_PERMITIDO", "false", "Permite expedir sem saldo",
                "BOOLEAN");
        inserirConfigSeNaoExistir(tenantJdbc, "IMPRESSAO_AUTOMATICA_ETIQUETA", "true", "Imprime etiqueta ao receber",
                "BOOLEAN");

        registrarTenantNoPool(tenantId);
        System.out.println(">>> Tenant " + tenantId + " provisionado!");
    }

    private void inserirConfigSeNaoExistir(JdbcTemplate jdbc, String chave, String valor, String desc, String tipo) {
        String check = "SELECT count(*) FROM tb_sistema_config WHERE chave = ?";
        Integer count = jdbc.queryForObject(check, Integer.class, chave);
        if (count == null || count == 0) {
            jdbc.update("INSERT INTO tb_sistema_config (chave, valor, descricao, tipo) VALUES (?, ?, ?, ?)", chave,
                    valor, desc, tipo);
        }
    }

    private void rodarMigracoes(String tenantId) {
        String tenantUrl = masterUrl.replace("wms_master", tenantId);
        if (tenantUrl.equals(masterUrl)) {
            tenantUrl = "jdbc:postgresql://wms-db-main:5432/" + tenantId;
        }
        Flyway flyway = Flyway.configure()
                .dataSource(tenantUrl, username, password)
                .locations("classpath:db/migration/tenant")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }

    // --- MELHORIA: ROLLBACK MANUAL ---
    public void dropDatabase(String tenantId) {
        try {
            // Força desconexão de usuários ativos para permitir o drop
            String killSql = """
                        SELECT pg_terminate_backend(pid)
                        FROM pg_stat_activity
                        WHERE datname = ? AND pid <> pg_backend_pid()
                    """;
            // Nota: JdbcTemplate padrão pode não conseguir rodar isso dependendo das
            // permissões,
            // mas num cenário padrão de dono do banco funciona.
            try {
                jdbcTemplate.queryForList(killSql, tenantId);
            } catch (Exception e) {
                /* Ignora se falhar ao matar conexões */ }

            String dropSql = "DROP DATABASE IF EXISTS " + tenantId;
            jdbcTemplate.execute(dropSql);
            System.out.println(">>> ROLLBACK: Banco " + tenantId + " removido devido a falha no processo.");
        } catch (Exception e) {
            System.err.println(">>> CRÍTICO: Falha ao fazer rollback do banco " + tenantId + ": " + e.getMessage());
        }
    }
}