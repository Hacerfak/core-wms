package br.com.hacerfak.coreWMS.core.config;

import br.com.hacerfak.coreWMS.core.multitenant.MultiTenantDataSource;
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
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

    /**
     * Define o Datasource "Padrão" (Master)
     * Usamos HikariDataSource para melhor performance (Connection Pooling)
     */
    @Bean
    public DataSource masterDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);

        // Configurações vitais para Pool
        dataSource.setPoolName("WmsMasterPool");
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(5);

        return dataSource;
    }

    /**
     * O Bean Mágico que troca de banco.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        MultiTenantDataSource routingDataSource = new MultiTenantDataSource();

        // Configura o Master como padrão
        DataSource masterDS = masterDataSource();

        Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
        targetDataSources.put(TenantContext.DEFAULT_TENANT_ID, masterDS);

        // --- HACK TEMPORÁRIO PARA TESTE ---
        // Vamos registrar o Tenant Demo manualmente aqui para vermos funcionar
        targetDataSources.put("wms_tenant_demo", createTenantDataSource("wms_tenant_demo"));
        // ----------------------------------

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDS);
        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }

    // Cria conexões novas para os Tenants
    private DataSource createTenantDataSource(String tenantDbName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);

        // Troca o nome do banco na URL
        String tenantUrl = url.replace("wms_master", tenantDbName);

        dataSource.setJdbcUrl(tenantUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSource.setPoolName("TenantPool-" + tenantDbName);
        dataSource.setMaximumPoolSize(10); // Menos conexões para tenants individuais

        return dataSource;
    }
}