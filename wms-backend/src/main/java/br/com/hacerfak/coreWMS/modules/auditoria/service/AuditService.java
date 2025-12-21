package br.com.hacerfak.coreWMS.modules.auditoria.service;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import br.com.hacerfak.coreWMS.modules.auditoria.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

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

    /**
     * Método para auditoria manual (usado em Batch Updates, Deletes ou falhas de
     * login)
     */
    @Async
    public void registrarLogManual(String acao, String entidade, String id, String detalhes, String usuario) {
        AuditLog log = AuditLog.builder()
                .entityName(entidade)
                .entityId(id)
                .action(acao)
                .tenantId(TenantContext.getTenant()) // Pega do contexto atual
                .usuario(usuario != null ? usuario : "SISTEMA")
                .dataHora(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                .conteudo(detalhes) // Pode passar uma String ou Map aqui
                .build();

        registrarLog(log);
    }
}