package br.com.hacerfak.coreWMS.modules.auditoria.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_audit_log", indexes = {
        @Index(name = "idx_audit_entidade", columnList = "entidade, entidadeId"),
        @Index(name = "idx_audit_data", columnList = "dataHora"),
        @Index(name = "idx_audit_tenant", columnList = "tenantId") // Index para filtrar por empresa se exportado
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId; // <--- O Vínculo da Empresa (Ex: "tenant_cocacola")

    @Column(nullable = false)
    private String evento; // CREATE, UPDATE, DELETE, LOGIN

    @Column(nullable = false)
    private String entidade; // Ex: "Produto"

    private String entidadeId; // ID do registro alterado

    private String usuario; // Quem fez (email)

    // --- RASTREABILIDADE (Onde e Como) ---
    private String ipOrigem;
    private String userAgent; // Navegador/Dispositivo

    @Builder.Default
    private LocalDateTime dataHora = LocalDateTime.now();

    // --- LOG INTELIGENTE (Apenas o que mudou) ---
    @Column(columnDefinition = "TEXT")
    private String dados; // JSON com o Diff: { "preco": { "de": 10, "para": 20 } }

    // Mantendo legado temporariamente (opcional)
    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String dadosAntigos;
    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String dadosNovos;
}