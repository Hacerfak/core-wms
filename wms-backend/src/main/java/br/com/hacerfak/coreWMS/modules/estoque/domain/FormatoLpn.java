package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "tb_formato_lpn", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "codigo" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FormatoLpn extends BaseEntity {

    @Column(nullable = false, length = 20, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    // Mapeamento explícito para 'tipo_base'
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_base", nullable = false)
    private TipoSuporte tipoBase;

    // --- Dimensões Físicas ---
    @Column(name = "altura_m", precision = 10, scale = 4)
    private BigDecimal alturaM;

    @Column(name = "largura_m", precision = 10, scale = 4)
    private BigDecimal larguraM;

    @Column(name = "profundidade_m", precision = 10, scale = 4)
    private BigDecimal profundidadeM;

    // --- Capacidades e Pesos ---

    @Column(name = "peso_suportado_kg", precision = 10, scale = 3)
    private BigDecimal pesoSuportadoKg;

    @Column(name = "tara_kg", precision = 10, scale = 3)
    private BigDecimal taraKg;

    @Builder.Default
    private boolean ativo = true;

    // --- Métodos Utilitários ---
    public BigDecimal getVolumeM3() {
        if (alturaM == null || larguraM == null || profundidadeM == null) {
            return BigDecimal.ZERO;
        }
        return alturaM.multiply(larguraM).multiply(profundidadeM).setScale(4, RoundingMode.HALF_UP);
    }
}