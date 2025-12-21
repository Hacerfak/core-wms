package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tb_movimento_estoque", indexes = {
        @Index(name = "idx_mov_produto", columnList = "produto_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentoEstoque extends BaseEntity {

    // ID herdado
    // Data Movimento removida (usar getCriadoEm da BaseEntity)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimento tipo;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne
    @JoinColumn(name = "localizacao_id", nullable = false)
    private Localizacao localizacao;

    private BigDecimal quantidade;

    @Column(length = 50)
    private String lpn;

    private String lote;
    private String numeroSerie;

    private String usuarioResponsavel; // Mantemos para exibir em tela, mas o BaseEntity terá criadoPor também
    private String observacao;
}