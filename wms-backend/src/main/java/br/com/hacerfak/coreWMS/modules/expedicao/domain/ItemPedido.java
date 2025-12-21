package br.com.hacerfak.coreWMS.modules.expedicao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "tb_item_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido extends BaseEntity {

    // ID herdado

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    @JsonIgnore
    private PedidoSaida pedido;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private BigDecimal quantidadeSolicitada;

    private BigDecimal quantidadeAlocada;
    private BigDecimal quantidadeSeparada;
}