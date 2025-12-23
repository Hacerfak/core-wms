package br.com.hacerfak.coreWMS.modules.expedicao.domain;

import br.com.hacerfak.coreWMS.core.domain.workflow.Tarefa;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder; // Importante

import java.math.BigDecimal;

@Entity
@Table(name = "tb_tarefa_separacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // <--- MUDANÇA
public class TarefaSeparacao extends Tarefa {

    @ManyToOne
    @JoinColumn(name = "onda_id", nullable = false)
    private OndaSeparacao onda;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne
    @JoinColumn(name = "origem_id", nullable = false)
    private Localizacao origem;

    @ManyToOne
    @JoinColumn(name = "destino_id")
    private Localizacao destino;

    private String loteSolicitado;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantidadePlanejada;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantidadeExecutada = BigDecimal.ZERO;
}