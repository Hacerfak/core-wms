package br.com.hacerfak.coreWMS.modules.expedicao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_item_volume_expedicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ItemVolumeExpedicao extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "volume_id", nullable = false)
    private VolumeExpedicao volume;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    private BigDecimal quantidade;
}