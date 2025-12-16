package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tb_estoque_saldo", uniqueConstraints = {
                @UniqueConstraint(name = "uk_estoque_saldo",
                                // Lembrando: Unique Constraint usando NULLS NOT DISTINCT (Postgres 15+)
                                columnNames = { "produto_id", "localizacao_id", "lote", "numero_serie" })
}, indexes = {
                @Index(name = "idx_estoque_produto", columnList = "produto_id"),
                @Index(name = "idx_estoque_local", columnList = "localizacao_id"),
                @Index(name = "idx_estoque_lote", columnList = "lote")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstoqueSaldo {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "produto_id", nullable = false)
        private Produto produto;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "localizacao_id", nullable = false)
        private Localizacao localizacao;

        // --- Rastreabilidade ---

        @Column(length = 50)
        private String lote;

        private LocalDate dataValidade;

        @Column(name = "numero_serie", length = 100)
        private String numeroSerie;

        // --- Quantitativo Físico ---

        @Column(nullable = false, precision = 18, scale = 4)
        private BigDecimal quantidade;

        // --- NOVO CAMPO: Quantitativo Lógico (Reserva) ---
        // Adicione isto para corrigir o erro
        @Builder.Default // Importante para o Builder não iniciar como null
        @Column(nullable = false, precision = 18, scale = 4)
        private BigDecimal quantidadeReservada = BigDecimal.ZERO;

        @Column(length = 50)
        private String lpn; // Pode ser NULL (se for produto solto)

        // --- Travamento Otimista ---
        @Version
        private Long version;

        // Método auxiliar útil para saber quanto está livre DE VERDADE
        public BigDecimal getQuantidadeDisponivel() {
                if (quantidade == null)
                        return BigDecimal.ZERO;
                BigDecimal reservada = quantidadeReservada != null ? quantidadeReservada : BigDecimal.ZERO;
                return quantidade.subtract(reservada);
        }
}