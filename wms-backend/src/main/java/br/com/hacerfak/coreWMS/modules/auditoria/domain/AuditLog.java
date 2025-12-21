package br.com.hacerfak.coreWMS.modules.auditoria.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@Document(collection = "audit_logs") // Coleção no Mongo
public class AuditLog {

    @Id
    private String id;

    private String entityName; // Ex: Produto, Parceiro
    private String entityId; // ID do registro (1, 2, 100...)
    private String action; // INSERT, UPDATE, DELETE

    private String tenantId; // De qual empresa foi essa alteração?
    private String usuario; // Quem fez?

    private LocalDateTime dataHora;

    // O "Snapshot" dos dados. O Mongo salva isso como um JSON flexível.
    private Object conteudo;
}