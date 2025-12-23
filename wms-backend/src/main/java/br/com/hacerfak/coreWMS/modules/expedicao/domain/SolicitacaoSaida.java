package br.com.hacerfak.coreWMS.modules.expedicao.domain;

import br.com.hacerfak.coreWMS.core.domain.workflow.Solicitacao;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder; // Importante

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_solicitacao_saida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // <--- MUDANÇA: Troque @Builder por @SuperBuilder
public class SolicitacaoSaida extends Solicitacao {

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Parceiro cliente;

    @Column(nullable = false)
    private Integer prioridade;

    @ManyToOne
    @JoinColumn(name = "onda_id")
    private OndaSeparacao onda;

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemSolicitacaoSaida> itens = new ArrayList<>();
}