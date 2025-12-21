package br.com.hacerfak.coreWMS.core.listener;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.core.util.BeanUtil;
import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import br.com.hacerfak.coreWMS.modules.auditoria.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class GlobalAuditListener {

    // ObjectMapper estático para evitar instanciar toda vez (leve e thread-safe)
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // Configurações para evitar erros de Lazy Loading e Datas
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // DICA: Se tiver o módulo Hibernate5Module no pom.xml, registre-o aqui para
        // ignorar Lazy:
        // mapper.registerModule(new Hibernate5Module());
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
            // 1. Obter dependência do Service via BeanUtil (pois Listener não é Bean
            // gerenciado)
            AuditService auditService = BeanUtil.getBean(AuditService.class);
            if (auditService == null)
                return; // Evita NullPointer na inicialização

            // 2. Identificar Usuário
            String usuario = "SISTEMA";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                usuario = auth.getName();
            }

            // 3. Identificar Tenant e ID
            String tenant = TenantContext.getTenant();
            String entityId = safeGetId(entity);

            // 4. Converter Entidade para Map/JSON Seguro
            // Isso resolve o problema de LazyInitializationException
            Object conteudoSeguro = converterParaMapSeguro(entity);

            // 5. Montar o Log
            AuditLog log = AuditLog.builder()
                    .entityName(entity.getClass().getSimpleName())
                    .entityId(entityId)
                    .action(action)
                    .tenantId(tenant)
                    .usuario(usuario)
                    .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                    .conteudo(conteudoSeguro)
                    .build();

            // 6. Enviar (Assíncrono)
            auditService.registrarLog(log);

        } catch (Exception e) {
            // Log de erro no console apenas, para não travar a transação principal
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
            // Tenta converter o objeto para um Map via Jackson
            // Se falhar por causa de Lazy Loading, cai no catch
            return mapper.convertValue(entity, Map.class);
        } catch (Exception e) {
            // Fallback: Se der erro de serialização, salva apenas uma String básica
            // Isso garante que o log é salvo mesmo que o conteúdo detalhado falhe
            Map<String, String> fallback = new HashMap<>();
            fallback.put("erro_serializacao", e.getMessage());
            fallback.put("dados_brutos", entity.toString());
            return fallback;
        }
    }
}