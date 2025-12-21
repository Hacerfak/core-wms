package br.com.hacerfak.coreWMS.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy; // Importante
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy; // Importante
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import br.com.hacerfak.coreWMS.core.listener.GlobalAuditListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners({ AuditingEntityListener.class, GlobalAuditListener.class })
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- QUEM FEZ ---
    @CreatedBy
    @Column(name = "criado_por", updatable = false)
    private String criadoPor;

    @LastModifiedBy
    @Column(name = "atualizado_por")
    private String atualizadoPor;

    // --- QUANDO FEZ ---
    @CreatedDate
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @Column(name = "data_finalizacao")
    private LocalDateTime dataFinalizacao;

    public boolean isAtivo() {
        return dataFinalizacao == null;
    }
}