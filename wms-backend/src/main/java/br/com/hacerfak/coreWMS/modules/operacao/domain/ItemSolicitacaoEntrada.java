package br.com.hacerfak.coreWMS.modules.operacao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_item_solicitacao_entrada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ItemSolicitacaoEntrada extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "solicitacao_id", nullable = false)
    @JsonIgnore // Evita ciclo infinito no JSON
    private SolicitacaoEntrada solicitacao;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    // Quantidade prevista no XML/Pedido
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantidadePrevista;

    // Quantidade efetivamente contada pelo operador
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantidadeConferida = BigDecimal.ZERO;

    // Se houver divergência de lote, pode ser útil guardar aqui também
    // (Embora o ideal seja o estoque final dizer o lote)
}