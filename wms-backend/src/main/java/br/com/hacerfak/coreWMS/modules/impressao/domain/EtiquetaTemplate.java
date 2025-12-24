package br.com.hacerfak.coreWMS.modules.impressao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tb_etiqueta_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EtiquetaTemplate extends BaseEntity {

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoFinalidadeEtiqueta tipoFinalidade;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String zplCodigo;

    private Integer larguraMm;
    private Integer alturaMm;

    @Builder.Default
    private boolean padrao = false;

    // Se nulo, é um template global do sistema. Se preenchido, é específico do
    // cliente.
    @ManyToOne
    @JoinColumn(name = "depositante_id")
    private Parceiro depositante;
}