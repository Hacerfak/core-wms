package br.com.hacerfak.coreWMS.modules.portaria.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.operacao.domain.SolicitacaoEntrada;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.SolicitacaoSaida;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_agendamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Agendamento extends BaseEntity {

    @Column(name = "codigo_reserva", unique = true)
    private String codigoReserva;

    @Enumerated(EnumType.STRING)
    private TipoAgendamento tipo; // ENTRADA, SAIDA

    @ManyToOne
    @JoinColumn(name = "turno_id")
    private Turno turno;

    @ManyToOne
    @JoinColumn(name = "transportadora_id")
    private Parceiro transportadora;

    @ManyToOne
    @JoinColumn(name = "motorista_id")
    private Parceiro motorista; // Se cadastrado

    @ManyToOne
    @JoinColumn(name = "doca_id")
    @JsonIgnoreProperties({ "area", "hibernateLazyInitializer", "handler" })
    private Localizacao doca;

    private LocalDateTime dataPrevistaInicio;
    private LocalDateTime dataPrevistaFim;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    // --- CORREÇÃO DO ERRO DE LAZY LOADING ---
    // Ignora as listas internas da solicitação ao serializar o agendamento
    @OneToOne
    @JoinColumn(name = "solicitacao_entrada_id")
    @JsonIgnoreProperties({ "itens", "tarefasConferencia", "hibernateLazyInitializer", "handler" })
    private SolicitacaoEntrada solicitacaoEntrada;

    @OneToOne
    @JoinColumn(name = "solicitacao_saida_id")
    @JsonIgnoreProperties({ "itens", "tarefas", "hibernateLazyInitializer", "handler" })
    private SolicitacaoSaida solicitacaoSaida;
    // -----------------------------------------

    @Builder.Default
    private boolean xmlVinculado = false;

    // Execução
    private LocalDateTime dataChegada;
    private LocalDateTime dataSaida;
    private String placaVeiculo;
    private String nomeMotoristaAvulso;
    private String cpfMotoristaAvulso;
}