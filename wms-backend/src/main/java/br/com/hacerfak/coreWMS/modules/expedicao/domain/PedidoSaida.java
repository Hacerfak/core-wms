package br.com.hacerfak.coreWMS.modules.expedicao.domain;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import br.com.hacerfak.coreWMS.core.domain.BaseEntity;

@Entity
@Table(name = "tb_pedido_saida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class PedidoSaida extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String codigoPedidoExterno; // ID do pedido no ERP/E-commerce

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Parceiro cliente; // Destinatário

    @Enumerated(EnumType.STRING)
    private StatusPedido status;

    private LocalDateTime dataFinalizacao;

    @Builder.Default
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();
}
