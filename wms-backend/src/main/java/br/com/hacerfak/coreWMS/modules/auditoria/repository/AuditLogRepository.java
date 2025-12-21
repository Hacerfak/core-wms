package br.com.hacerfak.coreWMS.modules.auditoria.repository;

import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    // Aqui poderemos criar consultas futuras, ex: buscar por entidade, por usuário,
    // etc.
    List<AuditLog> findByEntityNameAndEntityIdOrderByDataHoraDesc(String entityName, String entityId);
}