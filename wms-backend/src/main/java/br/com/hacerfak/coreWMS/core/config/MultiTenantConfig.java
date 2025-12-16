package br.com.hacerfak.coreWMS.core.config;

import br.com.hacerfak.coreWMS.core.multitenant.MultiTenantDataSource;
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class MultiTenantConfig {

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource masterDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setPoolName("WmsMasterPool");
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(5);
        return dataSource;
    }

    @Bean
    @Primary
    public MultiTenantDataSource dataSource(DataSource masterDataSource) { // REMOVIDO: EmpresaRepository
        MultiTenantDataSource routingDataSource = new MultiTenantDataSource();

        Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();

        // 1. Define o Master
        targetDataSources.put(TenantContext.DEFAULT_TENANT_ID, masterDataSource);

        // 2. Carrega tenants usando JDBC PURO (Para evitar ciclo com JPA)
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(masterDataSource);

            // Busca apenas os IDs dos tenants ativos
            String sql = "SELECT tenant_id FROM tb_empresa WHERE ativo = true";

            List<String> tenantIds = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("tenant_id"));

            for (String tenantId : tenantIds) {
                targetDataSources.put(tenantId, createTenantDataSource(tenantId));
            }

            System.out.println(">>> MULTI-TENANT: " + tenantIds.size() + " tenants carregados via JDBC.");

        } catch (Exception e) {
            // No primeiro boot (tabela não existe), é normal falhar. O Flyway vai criar
            // depois.
            System.err
                    .println(">>> Aviso: Não foi possível carregar tenants iniciais (Tabela pode não existir ainda).");
        }

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }

    public DataSource createTenantDataSource(String tenantDbName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);

        String tenantUrl = url.replace("wms_master", tenantDbName);
        if (tenantUrl.equals(url)) {
            tenantUrl = url.substring(0, url.lastIndexOf("/") + 1) + tenantDbName;
        }

        dataSource.setJdbcUrl(tenantUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setPoolName("TenantPool-" + tenantDbName);
        dataSource.setMaximumPoolSize(10);
        return dataSource;
    }
}