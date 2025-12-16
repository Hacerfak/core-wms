package br.com.hacerfak.coreWMS.modules.cadastro.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import br.com.hacerfak.coreWMS.core.domain.BaseEntity;

@Entity
@Table(name = "tb_produto", uniqueConstraints = {
                // O SKU agora é único por depositante
                @UniqueConstraint(name = "uk_produto_sku_depositante", columnNames = { "sku", "depositante_id" })
}, indexes = {
                @Index(name = "idx_produto_sku", columnList = "sku"),
                @Index(name = "idx_produto_ean", columnList = "ean13")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Produto extends BaseEntity {

        @Column(nullable = false, length = 50)
        private String sku;

        @Column(nullable = false)
        private String nome;

        @Column(name = "ean13", length = 13)
        private String ean13; // Código de Barras EAN-13

        @Column(name = "dun14", length = 14)
        private String dun14; // Código de Barras DUN-14

        @Column(name = "unidade_medida", length = 10)
        private String unidadeMedida; // UN, KG, CX

        // --- CORREÇÃO 1: Peso Padrão ---
        @Builder.Default
        @Column(precision = 10, scale = 3)
        private BigDecimal pesoBrutoKg = BigDecimal.ZERO;

        @Column(length = 10)
        private String ncm; // Classificação Fiscal

        @Column(length = 10)
        private String cest;

        @Column(precision = 18, scale = 4)
        private BigDecimal valorUnitarioPadrao;

        // Quem é o dono desse cadastro?
        @ManyToOne(optional = false)
        @JoinColumn(name = "depositante_id", nullable = false)
        private Parceiro depositante;

        // --- CORREÇÃO 2: Ativo por Padrão ---
        @Builder.Default
        @Column(nullable = false)
        private boolean ativo = true;

        @Builder.Default
        private boolean controlaLote = false;
        @Builder.Default
        private boolean controlaValidade = false;
        @Builder.Default
        private boolean controlaSerie = false;

        // Conversão
        private String unidadeArmazenagem; // Ex: CX
        @Builder.Default
        private Integer fatorConversao = 1; // Ex: 12 (1 CX = 12 UN)
}