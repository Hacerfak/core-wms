package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.workflow.Tarefa;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_tarefa_movimentacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TarefaMovimentacao extends Tarefa {

    @Enumerated(EnumType.STRING)
    private TipoMovimentoInterno tipoMovimento; // RESSUPRIMENTO, CONSOLIDACAO, MANUAL

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne
    @JoinColumn(name = "origem_id", nullable = false)
    private Localizacao origem;

    @ManyToOne
    @JoinColumn(name = "destino_id", nullable = false)
    private Localizacao destino;

    private BigDecimal quantidade;

    // Opcional: LPN Específica se for movimentar um pallet inteiro
    @ManyToOne
    @JoinColumn(name = "lpn_id")
    private Lpn lpn;
}