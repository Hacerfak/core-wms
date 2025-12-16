package br.com.hacerfak.coreWMS.modules.operacao.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import br.com.hacerfak.coreWMS.core.domain.BaseEntity;

@Entity
@Table(name = "tb_recebimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Recebimento extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "num_nota_fiscal")
    private String numNotaFiscal;

    @Column(name = "chave_acesso", length = 44)
    private String chaveAcesso;

    private String fornecedor;

    @Enumerated(EnumType.STRING)
    private StatusRecebimento status;

    @Column(name = "data_emissao")
    private LocalDateTime dataEmissao;

    private LocalDateTime dataFinalizacao;

    // Cascade: Se salvar o Recebimento, salva os itens automaticamente
    @Builder.Default
    @OneToMany(mappedBy = "recebimento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemRecebimento> itens = new ArrayList<>();
}
