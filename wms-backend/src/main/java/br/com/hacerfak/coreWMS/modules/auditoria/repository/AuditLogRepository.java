package br.com.hacerfak.coreWMS.modules.auditoria.repository;

import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    // Método de limpeza (já estava correto com dataHora)
    void deleteByDataHoraBefore(LocalDateTime dataLimite);
}