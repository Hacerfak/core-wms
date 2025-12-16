package br.com.hacerfak.coreWMS.modules.fiscal.domain;

import br.com.hacerfak.coreWMS.modules.expedicao.domain.PedidoSaida;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_nota_fiscal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Vincula 1 Nota a 1 Pedido
    @OneToOne
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private PedidoSaida pedido;

    @Column(length = 44, unique = true)
    private String chaveAcesso; // A chave de 44 dígitos da SEFAZ

    private Integer numero;
    private Integer serie;

    @Enumerated(EnumType.STRING)
    private StatusNfe status; // AUTORIZADA, REJEITADA, PROCESSANDO

    @Column(columnDefinition = "TEXT")
    private String xmlAssinado; // Backup do que enviamos

    @Column(columnDefinition = "TEXT")
    private String xmlProtocolo; // O recibo da SEFAZ (obrigatório guardar)

    private String motivoRejeicao; // Se der erro, salva aqui

    private LocalDateTime dataEmissao;
}
