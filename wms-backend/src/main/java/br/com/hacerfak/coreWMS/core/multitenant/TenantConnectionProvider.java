package br.com.hacerfak.coreWMS.core.multitenant;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.EmpresaRepository;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TenantConnectionProvider {

    private final EmpresaRepository empresaRepository;

    // Armazena as conexões vivas em memória
    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    @Value("${spring.datasource.url}")
    private String masterUrl;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    /**
     * Roda automaticamente quando o sistema sobe.
     * Lê o banco Master e cria conexões para cada empresa existente.
     */
    @PostConstruct
    public void init() {
        try {
            List<Empresa> empresas = empresaRepository.findAll();
            for (Empresa empresa : empresas) {
                addTenantConnection(empresa.getTenantId());
            }
            System.out.println(">>> MULTI-TENANT: Carregados " + empresas.size() + " bancos de empresas.");
        } catch (Exception e) {
            // Em ambiente zero (primeira vez), a tabela pode não existir ainda
            System.err.println(">>> Aviso: Não foi possível carregar tenants iniciais (pode ser a primeira execução).");
        }
    }

    /**
     * Adiciona uma nova conexão ao pool dinamicamente.
     * Chamaremos isso no futuro quando o Admin fizer o Upload do Certificado.
     */
    public DataSource addTenantConnection(String tenantId) {
        if (dataSources.containsKey(tenantId)) {
            return dataSources.get(tenantId);
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setJdbcUrl(masterUrl.replace("wms_master", tenantId)); // Troca o nome do banco
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setPoolName("TenantPool-" + tenantId);
        ds.setMaximumPoolSize(5); // Pool menor para tenants

        dataSources.put(tenantId, ds);
        return ds;
    }

    public Map<String, DataSource> getAllKnownDataSources() {
        return dataSources;
    }
}