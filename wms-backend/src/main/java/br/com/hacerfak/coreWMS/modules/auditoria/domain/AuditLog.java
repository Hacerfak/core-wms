package br.com.hacerfak.coreWMS.modules.auditoria.domain;

import lombok.*;
// IMPORTANTE: Use o Id do Spring Data, não do Jakarta Persistence
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "tb_audit_log") // Define que é um Documento Mongo
@CompoundIndexes({
        // Índices compostos equivalentes aos que você tinha no JPA
        @CompoundIndex(name = "idx_audit_entidade", def = "{'entidade': 1, 'entidadeId': 1}"),
        @CompoundIndex(name = "idx_audit_tenant", def = "{'tenantId': 1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private String id; // MUDANÇA CRÍTICA: Mongo usa String (ObjectId)

    @Indexed // Índice simples para busca rápida por tenant (opcional se já estiver no
             // composto)
    private String tenantId;

    private String evento; // CREATE, UPDATE, DELETE, LOGIN

    private String entidade; // Ex: "Produto"

    private String entidadeId; // ID do registro alterado

    private String usuario; // Quem fez (email)

    // --- RASTREABILIDADE (Onde e Como) ---
    private String ipOrigem;
    private String userAgent;

    @Builder.Default
    @Indexed(name = "idx_audit_data", direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING)
    private LocalDateTime dataHora = LocalDateTime.now();

    // --- LOG INTELIGENTE ---
    // No Mongo não precisamos de @Column(columnDefinition = "TEXT"), ele suporta
    // Strings grandes nativamente
    private String dados;
}