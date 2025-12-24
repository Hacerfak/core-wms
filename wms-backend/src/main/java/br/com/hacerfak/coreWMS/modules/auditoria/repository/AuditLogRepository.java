package br.com.hacerfak.coreWMS.modules.auditoria.repository;

import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    // --- CORREÇÃO: Renomeado de EntityName/EntityId para Entidade/EntidadeId ---
    List<AuditLog> findByEntidadeAndEntidadeIdOrderByDataHoraDesc(String entidade, String entidadeId);

    // Método de limpeza (já estava correto com dataHora)
    void deleteByDataHoraBefore(LocalDateTime dataLimite);
}