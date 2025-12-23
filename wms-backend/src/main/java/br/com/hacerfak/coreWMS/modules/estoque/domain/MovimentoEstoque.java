package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_movimento_estoque", indexes = {
        @Index(name = "idx_mov_produto", columnList = "produto_id"),
        @Index(name = "idx_mov_lpn", columnList = "lpn"),
        @Index(name = "idx_mov_data", columnList = "data_criacao")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // Atualizado para SuperBuilder
public class MovimentoEstoque extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimento tipo;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne
    @JoinColumn(name = "localizacao_id", nullable = false)
    private Localizacao localizacao;

    // A quantidade movimentada (Delta)
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantidade;

    // --- NOVO: SNAPSHOT PARA AUDITORIA/KARDEX ---
    @Column(name = "saldo_anterior", precision = 18, scale = 4)
    private BigDecimal saldoAnterior;

    @Column(name = "saldo_atual", precision = 18, scale = 4)
    private BigDecimal saldoAtual;
    // --------------------------------------------

    @Column(length = 50)
    private String lpn;

    private String lote;
    private String numeroSerie;

    private String usuarioResponsavel;
    private String observacao;
}