package br.com.hacerfak.coreWMS.modules.expedicao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_item_solicitacao_saida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemSolicitacaoSaida extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "solicitacao_id", nullable = false)
    @JsonIgnore
    private SolicitacaoSaida solicitacao;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantidadeSolicitada;

    // Quanto o sistema já reservou para este item
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantidadeAlocada = BigDecimal.ZERO;

    // Quanto já foi separado (picked)
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantidadeSeparada = BigDecimal.ZERO;
}