package br.com.hacerfak.coreWMS.modules.auditoria.service;

import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import br.com.hacerfak.coreWMS.modules.auditoria.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async // Executa em thread separada
    public void registrarLog(AuditLog log) {
        try {
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Em caso de falha no log, não queremos parar o WMS, apenas printar o erro.
            System.err.println("FALHA AO SALVAR AUDITORIA NO MONGO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}