package br.com.hacerfak.coreWMS.modules.operacao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
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
public class ItemRecebimento extends BaseEntity {

    // ID herdado

    @ManyToOne
    @JoinColumn(name = "recebimento_id", nullable = false)
    @JsonIgnore
    private Recebimento recebimento;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private BigDecimal quantidadeNota;

    @Column(nullable = false)
    private BigDecimal quantidadeConferida;

    private String loteConferido;
}