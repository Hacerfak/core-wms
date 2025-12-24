package br.com.hacerfak.coreWMS.modules.auditoria.repository;

import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    // Aqui poderemos criar consultas futuras, ex: buscar por entidade, por usuário,
    // etc.
    List<AuditLog> findByEntityNameAndEntityIdOrderByDataHoraDesc(String entityName, String entityId);

    // --- NOVO MÉTODO PARA LIMPEZA ---
    @Modifying
    @Transactional
    void deleteByDataHoraBefore(LocalDateTime dataLimite);
}