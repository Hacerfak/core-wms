package br.com.hacerfak.coreWMS.modules.inventario.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Inventario extends BaseEntity {

    private String descricao; // Ex: "Rotativo Rua A - Outubro"

    @Enumerated(EnumType.STRING)
    private TipoInventario tipo;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusInventario status = StatusInventario.ABERTO;

    private LocalDate dataAgendada;

    // Configurações
    private boolean cego; // Operador vê o saldo?
    private Integer maxTentativas; // 1, 2 ou 3 contagens

    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL)
    @Builder.Default
    private List<TarefaContagem> tarefas = new ArrayList<>();
}