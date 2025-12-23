package br.com.hacerfak.coreWMS.modules.faturamento.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tb_servico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Servico extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String codigo; // ARM-PALLET, IN-REC-CX

    private String nome;

    private String unidadeMedida; // PALLET, CX, UN

    @Enumerated(EnumType.STRING)
    private TipoCobranca tipoCobranca;

    @Builder.Default
    private boolean ativo = true;
}