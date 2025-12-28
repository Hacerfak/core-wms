package br.com.hacerfak.coreWMS.modules.expedicao.domain;

import br.com.hacerfak.coreWMS.core.domain.workflow.Solicitacao;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.portaria.domain.Agendamento;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder; // Importante

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

    @Column(length = 50)
    private String rota; // Ex: "ZONA_SUL", "ROTA_01"

    @Column(name = "sequencia_entrega")
    private Integer sequenciaEntrega; // 1, 2, 3... (Ordem de parada do caminhão)

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemSolicitacaoSaida> itens = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "doca_id")
    @JsonIgnoreProperties({ "area", "hibernateLazyInitializer", "handler" })
    private Localizacao doca;

    // --- NOVO: Mapeamento reverso para facilitar DTOs ---
    @OneToOne(mappedBy = "solicitacaoSaida")
    @JsonIgnore // Evita ciclo
    private Agendamento agendamento;
}