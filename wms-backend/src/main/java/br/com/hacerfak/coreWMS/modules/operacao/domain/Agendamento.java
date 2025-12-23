package br.com.hacerfak.coreWMS.modules.operacao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
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
    private String codigoReserva; // Ex: AG-202310-001

    @ManyToOne
    @JoinColumn(name = "transportadora_id")
    private Parceiro transportadora;

    @ManyToOne
    @JoinColumn(name = "motorista_id") // Pode ser parceiro ou campo texto solto se for avulso
    private Parceiro motorista;

    @ManyToOne
    @JoinColumn(name = "doca_id")
    private Localizacao doca; // Qual doca foi reservada

    private LocalDateTime dataPrevistaInicio;
    private LocalDateTime dataPrevistaFim;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusAgendamento status = StatusAgendamento.AGENDADO;

    // Vínculo com a Entrada (Opcional, pois pode ser agendamento de Saída)
    @OneToOne
    @JoinColumn(name = "solicitacao_entrada_id")
    private SolicitacaoEntrada solicitacaoEntrada;

    // Dados de Execução (Portaria preenche)
    private LocalDateTime dataChegada; // Check-in
    private LocalDateTime dataSaida; // Check-out
    private String placaVeiculo;
    private String nomeMotoristaAvulso;
    private String cpfMotoristaAvulso;
}