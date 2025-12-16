package br.com.hacerfak.coreWMS.core.domain;

import jakarta.persistence.*; // Importa Id, GeneratedValue, GenerationType, etc.
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @LastModifiedDate
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    // --- O CAMPO NOVO ---
    // Usaremos para Soft Delete (se estiver preenchido, o registro foi "excluído")
    @Column(name = "data_finalizacao")
    private LocalDateTime dataFinalizacao;

    /**
     * Método utilitário para saber se está ativo
     */
    public boolean isAtivo() {
        return dataFinalizacao == null;
    }
}