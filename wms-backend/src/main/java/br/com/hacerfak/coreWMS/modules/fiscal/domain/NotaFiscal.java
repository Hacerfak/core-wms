package br.com.hacerfak.coreWMS.modules.fiscal.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
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
public class NotaFiscal extends BaseEntity {

    // ID herdado

    @OneToOne
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private PedidoSaida pedido;

    @Column(length = 44, unique = true)
    private String chaveAcesso;

    private Integer numero;
    private Integer serie;

    @Enumerated(EnumType.STRING)
    private StatusNfe status;

    @Column(columnDefinition = "TEXT")
    private String xmlAssinado;

    @Column(columnDefinition = "TEXT")
    private String xmlProtocolo;

    private String motivoRejeicao;

    private LocalDateTime dataEmissao;
}