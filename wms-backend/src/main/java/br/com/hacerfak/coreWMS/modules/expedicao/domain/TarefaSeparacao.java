package br.com.hacerfak.coreWMS.modules.expedicao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tb_tarefa_separacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarefaSeparacao extends BaseEntity {

    // ID herdado

    @ManyToOne
    @JsonIgnoreProperties("itens")
    private PedidoSaida pedido;

    @ManyToOne
    private Produto produto;

    @ManyToOne
    private Localizacao localizacaoOrigem;

    private String loteAlocado;

    @Column(nullable = false)
    private BigDecimal quantidadePlanejada;

    @Builder.Default
    @Column(nullable = false)
    private boolean concluida = false;
}