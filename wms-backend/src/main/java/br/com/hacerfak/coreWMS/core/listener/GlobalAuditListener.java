package br.com.hacerfak.coreWMS.core.listener;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.core.util.BeanUtil;
import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import br.com.hacerfak.coreWMS.modules.auditoria.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.hibernate7.Hibernate7Module;
import jakarta.persistence.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class GlobalAuditListener {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // Configurações para evitar erros de Lazy Loading e Datas
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // --- CORREÇÃO: REGISTRO DO MÓDULO HIBERNATE ---
        // Isso impede que o Jackson tente serializar proxies Lazy e entre em loop
        // infinito
        // ATENÇÃO: Se estiver usando Hibernate 7
        Hibernate7Module hibernateModule = new Hibernate7Module();

        // Configuração para não travar a auditoria com Lazy Loading
        hibernateModule.configure(Hibernate7Module.Feature.FORCE_LAZY_LOADING, false);
        mapper.registerModule(hibernateModule);
    }

    @PostPersist
    public void onPostPersist(Object entity) {
        enviarLog("INSERT", entity);
    }

    @PostUpdate
    public void onPostUpdate(Object entity) {
        enviarLog("UPDATE", entity);
    }

    @PostRemove
    public void onPostRemove(Object entity) {
        enviarLog("DELETE", entity);
    }

    private void enviarLog(String action, Object entity) {
        try {
            AuditService auditService = BeanUtil.getBean(AuditService.class);
            if (auditService == null)
                return;

            String usuario = "SISTEMA";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                usuario = auth.getName();
            }

            String tenant = TenantContext.getTenant();
            String entityId = safeGetId(entity);

            // Conversão agora é segura graças ao HibernateModule
            Object conteudoSeguro = converterParaMapSeguro(entity);

            AuditLog log = AuditLog.builder()
                    .entityName(entity.getClass().getSimpleName())
                    .entityId(entityId)
                    .action(action)
                    .tenantId(tenant)
                    .usuario(usuario)
                    .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                    .conteudo(conteudoSeguro)
                    .build();

            auditService.registrarLog(log);

        } catch (Exception e) {
            System.err.println("ERRO AUDITORIA (" + entity.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    private String safeGetId(Object entity) {
        try {
            return String.valueOf(entity.getClass().getMethod("getId").invoke(entity));
        } catch (Exception e) {
            return "N/A";
        }
    }

    private Object converterParaMapSeguro(Object entity) {
        try {
            return mapper.convertValue(entity, Map.class);
        } catch (Exception e) {
            Map<String, String> fallback = new HashMap<>();
            fallback.put("erro_serializacao", e.getMessage());
            fallback.put("dados_brutos", entity.toString());
            return fallback;
        }
    }
}