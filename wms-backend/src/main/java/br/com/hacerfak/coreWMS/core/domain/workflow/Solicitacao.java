package br.com.hacerfak.coreWMS.core.domain.workflow;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder; // Importante

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder // <--- MUDANÇA
@NoArgsConstructor
@AllArgsConstructor
public abstract class Solicitacao extends BaseEntity {

    @Column(name = "codigo_externo", length = 50)
    private String codigoExterno;

    @Column(name = "data_limite")
    private LocalDateTime dataLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusSolicitacao status;

    public boolean isConcluida() {
        return StatusSolicitacao.CONCLUIDA.equals(this.status) || StatusSolicitacao.CANCELADA.equals(this.status);
    }
}