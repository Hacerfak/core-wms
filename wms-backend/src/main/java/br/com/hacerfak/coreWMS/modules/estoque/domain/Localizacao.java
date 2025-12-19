package br.com.hacerfak.coreWMS.modules.estoque.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import br.com.hacerfak.coreWMS.core.domain.BaseEntity;

@Entity
@Table(name = "tb_localizacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Localizacao extends BaseEntity {

    // --- VÍNCULO NOVO ---
    @ManyToOne(optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false, length = 20)
    private String codigo; // O sufixo (ex: 01-02-03)

    @Column(nullable = false, unique = true)
    private String enderecoCompleto; // Calculado (ex: CD1-RUA1-01-02-03)

    private String descricao;

    @Enumerated(EnumType.STRING)
    private TipoLocalizacao tipo; // Pode sobrescrever o tipo da área

    // --- REGRAS ---
    @Builder.Default
    private boolean virtual = false;

    @Builder.Default
    private boolean permiteMultiLpn = true;

    @Builder.Default
    private Integer capacidadeLpn = 1;

    @Column(precision = 19, scale = 4)
    private BigDecimal capacidadePesoKg;

    @Builder.Default
    private boolean bloqueado = false;

    @Builder.Default
    private boolean ativo = true;

    // --- LÓGICA DE CONCATENAÇÃO AUTOMÁTICA ---
    @PrePersist
    @PreUpdate
    public void gerarEnderecoCompleto() {
        if (this.area != null && this.area.getArmazem() != null) {
            // Formato: ARMAZEM + AREA + POSICAO (Tudo junto, sem traço)
            // Ex: CD01DOCREC
            this.enderecoCompleto = String.format("%s%s%s",
                    this.area.getArmazem().getCodigo().trim(),
                    this.area.getCodigo().trim(),
                    this.codigo.trim()).toUpperCase();
        }
    }
}