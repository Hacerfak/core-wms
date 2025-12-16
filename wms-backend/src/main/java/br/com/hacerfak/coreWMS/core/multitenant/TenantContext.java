package br.com.hacerfak.coreWMS.core.multitenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantContext {
    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);

    // O valor padrão é sempre o MASTER (banco de login/admin)
    public static final String DEFAULT_TENANT_ID = "wms_master";

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setTenant(String tenantId) {
        log.debug("Definindo contexto do tenant para: {}", tenantId);
        currentTenant.set(tenantId);
    }

    public static String getTenant() {
        String tenant = currentTenant.get();
        return tenant != null ? tenant : DEFAULT_TENANT_ID;
    }

    public static void clear() {
        currentTenant.remove();
    }
}