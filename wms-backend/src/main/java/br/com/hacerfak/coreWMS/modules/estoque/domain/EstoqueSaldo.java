package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tb_estoque_saldo", uniqueConstraints = {
                @UniqueConstraint(name = "uk_estoque_saldo", columnNames = { "produto_id", "localizacao_id", "lote",
                                "numero_serie" })
}, indexes = {
                @Index(name = "idx_estoque_produto", columnList = "produto_id"),
                @Index(name = "idx_estoque_local", columnList = "localizacao_id"),
                @Index(name = "idx_estoque_lote", columnList = "lote")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EstoqueSaldo extends BaseEntity {

        // ID herdado

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "produto_id", nullable = false)
        private Produto produto;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "localizacao_id", nullable = false)
        private Localizacao localizacao;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        @Builder.Default
        private StatusQualidade statusQualidade = StatusQualidade.DISPONIVEL;

        // --- Rastreabilidade ---
        @Column(length = 50)
        private String lote;
        private LocalDate dataValidade;
        @Column(name = "numero_serie", length = 100)
        private String numeroSerie;

        // --- Quantitativo ---
        @Column(nullable = false, precision = 18, scale = 4)
        private BigDecimal quantidade;

        @Builder.Default
        @Column(nullable = false, precision = 18, scale = 4)
        private BigDecimal quantidadeReservada = BigDecimal.ZERO;

        @Column(length = 50)
        private String lpn;

        @Version
        private Long version;

        public BigDecimal getQuantidadeDisponivel() {
                if (quantidade == null)
                        return BigDecimal.ZERO;
                BigDecimal reservada = quantidadeReservada != null ? quantidadeReservada : BigDecimal.ZERO;
                return quantidade.subtract(reservada);
        }
}