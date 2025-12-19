package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_armazem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Armazem extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    private String enderecoCompleto;

    @Builder.Default
    private boolean ativo = true;
}