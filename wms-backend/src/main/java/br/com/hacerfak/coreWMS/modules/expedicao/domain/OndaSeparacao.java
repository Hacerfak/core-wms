package br.com.hacerfak.coreWMS.modules.expedicao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_onda_separacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OndaSeparacao extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String codigo; // Ex: ONDA-20231025-01

    @Enumerated(EnumType.STRING)
    private StatusOnda status; // CRIADA, LIBERADA, EM_SEPARACAO, CONCLUIDA

    private LocalDateTime dataLiberacao;

    // Uma onda tem várias solicitações
    @OneToMany(mappedBy = "onda")
    @Builder.Default
    private List<SolicitacaoSaida> solicitacoes = new ArrayList<>();

    // Uma onda gera várias tarefas de picking
    @OneToMany(mappedBy = "onda", cascade = CascadeType.ALL)
    @Builder.Default
    private List<TarefaSeparacao> tarefas = new ArrayList<>();
}