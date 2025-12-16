package br.com.hacerfak.coreWMS.core.multitenant;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiTenantDataSource extends AbstractRoutingDataSource {

    // Mantemos nossa própria lista de DataSources para poder adicionar novos depois
    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenant();
    }

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        // Atualiza nosso mapa local e passa para o pai (Spring)
        this.targetDataSources.putAll(targetDataSources);
        super.setTargetDataSources(this.targetDataSources);
    }

    /**
     * Permite adicionar novos Tenants em tempo de execução (Onboarding)
     * e força o Spring a reconhecê-los imediatamente.
     */
    public void addTenant(String tenantId, DataSource dataSource) {
        this.targetDataSources.put(tenantId, dataSource);

        // Avisa o Spring que a lista mudou
        super.setTargetDataSources(this.targetDataSources);
        super.afterPropertiesSet();
    }
}