package br.com.hacerfak.coreWMS.modules.expedicao.domain;

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
public class TarefaSeparacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties("itens") // Evita loop infinito ao serializar o pedido
    private PedidoSaida pedido;

    @ManyToOne
    private Produto produto;

    // DE ONDE TIRAR (O sistema escolheu isso via FEFO)
    @ManyToOne
    private Localizacao localizacaoOrigem;

    // DADOS ESPECÍFICOS PARA GARANTIR RASTREABILIDADE
    private String loteAlocado;

    @Column(nullable = false)
    private BigDecimal quantidadePlanejada;

    // --- CORREÇÃO AQUI ---
    // Adicionamos @Builder.Default para garantir que o valor comece como 'false'
    @Builder.Default
    @Column(nullable = false)
    private boolean concluida = false;
}