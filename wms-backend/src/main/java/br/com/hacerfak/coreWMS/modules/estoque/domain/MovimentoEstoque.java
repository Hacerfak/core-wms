package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_movimento_estoque", indexes = {
        @Index(name = "idx_mov_data", columnList = "dataMovimento"),
        @Index(name = "idx_mov_produto", columnList = "produto_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimento tipo;

    @CreationTimestamp
    private LocalDateTime dataMovimento;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne
    @JoinColumn(name = "localizacao_id", nullable = false)
    private Localizacao localizacao;

    private BigDecimal quantidade;

    // --- CORREÇÃO: Adicionado o campo LPN aqui ---
    @Column(length = 50)
    private String lpn;
    // ---------------------------------------------

    private String lote;
    private String numeroSerie;

    private String usuarioResponsavel;
    private String observacao;
}