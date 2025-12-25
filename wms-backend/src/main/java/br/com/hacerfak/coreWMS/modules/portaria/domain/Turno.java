package br.com.hacerfak.coreWMS.modules.portaria.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalTime;

@Entity
@Table(name = "tb_turno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Turno extends BaseEntity {
    private String nome;
    private LocalTime inicio;
    private LocalTime fim;
    private String diasSemana; // CSV: SEG,TER...
    @Builder.Default
    private boolean ativo = true;
}