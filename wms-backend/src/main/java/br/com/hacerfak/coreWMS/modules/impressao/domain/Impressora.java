package br.com.hacerfak.coreWMS.modules.impressao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Armazem;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tb_impressora")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Impressora extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String nome;

    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoConexaoImpressora tipoConexao;

    // Configurações de Rede
    private String enderecoIp;

    @Builder.Default
    private Integer porta = 9100;

    // Configurações de Share/USB
    private String caminhoCompartilhamento;

    @Builder.Default
    private boolean ativo = true;

    @ManyToOne
    @JoinColumn(name = "armazem_id")
    private Armazem armazem;

    @ManyToOne
    @JoinColumn(name = "depositante_id")
    private Parceiro depositante;
}