package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tb_lpn_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LpnItem extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "lpn_id", nullable = false)
    @JsonIgnore
    private Lpn lpn;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantidade;

    // Rastreabilidade granular por item dentro do pallet
    private String lote;
    private LocalDate dataValidade;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusQualidade statusQualidade = StatusQualidade.DISPONIVEL;
}