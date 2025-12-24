package br.com.hacerfak.coreWMS.core.listener;

import br.com.hacerfak.coreWMS.core.util.BeanUtil;
import br.com.hacerfak.coreWMS.modules.auditoria.service.AuditService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener JPA global para capturar mudanças nas entidades automaticamente.
 * Refatorado para usar o AuditService centralizado.
 */
@Slf4j
public class GlobalAuditListener {

    @PostPersist
    public void onPersist(Object entity) {
        // CREATE: Antigo = null, Novo = entity
        enviarParaAuditoria("CREATE", null, entity);
    }

    @PostUpdate
    public void onUpdate(Object entity) {
        // UPDATE: Limitação do JPA - Não temos acesso fácil ao estado "Antigo" aqui.
        // Passamos 'null' como antigo, o que fará o DiffUtils gerar um snapshot
        // completo do estado atual.
        // Para Diffs precisos (De -> Para), prefira chamar
        // auditService.registrarAuditoria() na camada de Service.
        enviarParaAuditoria("UPDATE", null, entity);
    }

    @PostRemove
    public void onRemove(Object entity) {
        // DELETE: Antigo = entity, Novo = null
        enviarParaAuditoria("DELETE", entity, null);
    }

    private void enviarParaAuditoria(String evento, Object antigo, Object novo) {
        try {
            // Como EntityListeners não são gerenciados pelo Spring, usamos o BeanUtil para
            // pegar o Service
            AuditService auditService = BeanUtil.getBean(AuditService.class);

            if (auditService == null) {
                log.warn("AuditService não encontrado. Auditoria ignorada para: {}", evento);
                return;
            }

            Object alvo = (novo != null) ? novo : antigo;
            String entidadeId = safeGetId(alvo);

            // Delega para o serviço que já cuida do Diff, Tenant, IP e User-Agent
            auditService.registrarAuditoria(evento, alvo, entidadeId, antigo, novo);

        } catch (Exception e) {
            log.error("Erro ao processar auditoria automática para entidade: " +
                    (novo != null ? novo.getClass().getSimpleName() : "Desconhecido"), e);
        }
    }

    private String safeGetId(Object entity) {
        if (entity == null)
            return "N/A";
        try {
            // Tenta obter o ID via Reflection (assume método getId padrão)
            Object id = entity.getClass().getMethod("getId").invoke(entity);
            return String.valueOf(id);
        } catch (Exception e) {
            return "ID_DESCONHECIDO";
        }
    }
}