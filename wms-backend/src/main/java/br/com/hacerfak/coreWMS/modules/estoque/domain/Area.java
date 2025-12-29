package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tb_area", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "armazem_id", "codigo" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Area extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "armazem_id", nullable = false)
    private Armazem armazem;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @Builder.Default
    private boolean ativo = true;
}