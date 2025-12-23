package br.com.hacerfak.coreWMS.modules.expedicao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_volume_expedicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VolumeExpedicao extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoSaida solicitacao; // A qual pedido este volume pertence

    @Column(nullable = false, unique = true)
    private String codigoRastreio; // Etiqueta de envio (Shipping Label / SSCC)

    private String tipoEmbalagem; // CAIXA_M, PALLET, ENVELOPE

    private BigDecimal pesoBruto;

    private boolean fechado; // Se já foi lacrado

    @OneToMany(mappedBy = "volume", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ItemVolumeExpedicao> itens = new ArrayList<>();
}