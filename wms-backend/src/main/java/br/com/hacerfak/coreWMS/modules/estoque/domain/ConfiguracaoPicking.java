package br.com.hacerfak.coreWMS.modules.estoque.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_configuracao_picking", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "produto_id", "localizacao_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ConfiguracaoPicking extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne
    @JoinColumn(name = "localizacao_id", nullable = false)
    private Localizacao localizacao; // Deve ser um endereço de chão/picking

    @Column(nullable = false)
    private BigDecimal pontoRessuprimento; // "Mínimo": Se cair abaixo disso, chama reposição

    @Column(nullable = false)
    private BigDecimal capacidadeMaxima; // "Máximo": Para não mandar baixar mais do que cabe

    @Builder.Default
    private boolean ativo = true;
}