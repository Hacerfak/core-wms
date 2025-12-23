package br.com.hacerfak.coreWMS.core.domain;

import br.com.hacerfak.coreWMS.core.listener.GlobalAuditListener;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder; // Importante
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners({ AuditingEntityListener.class, GlobalAuditListener.class })
@Getter
@Setter
@SuperBuilder // <--- MUDANÇA: Permite herança no builder
@NoArgsConstructor // Necessário para o JPA
@AllArgsConstructor
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedBy
    @Column(name = "criado_por", updatable = false)
    private String criadoPor;

    @LastModifiedBy
    @Column(name = "atualizado_por")
    private String atualizadoPor;

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