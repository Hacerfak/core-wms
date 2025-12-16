package br.com.hacerfak.coreWMS.modules.operacao.domain;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "tb_item_recebimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemRecebimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recebimento_id", nullable = false)
    @JsonIgnore
    private Recebimento recebimento;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    // O QUE O XML DISSE (O Operador NÃO VÊ isso no app)
    @Column(nullable = false)
    private BigDecimal quantidadeNota;

    // O QUE O OPERADOR CONTOU
    @Column(nullable = false)
    private BigDecimal quantidadeConferida;

    // Dados capturados na conferência (para criar o lote depois)
    private String loteConferido;
}
