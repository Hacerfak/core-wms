package br.com.hacerfak.coreWMS.modules.operacao.domain;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_volume_recebimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolumeRecebimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recebimento_id", nullable = false)
    private Recebimento recebimento;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, unique = true)
    private String lpn; // O ID da Etiqueta Gerada

    @Column(nullable = false)
    private BigDecimal quantidadeOriginal;

    // Controle de Armazenagem
    @Builder.Default
    private boolean armazenado = false;

    @ManyToOne
    @JoinColumn(name = "local_destino_id")
    private Localizacao localDestino; // Preenchido só na hora de guardar

    private LocalDateTime dataCriacao;
    private String usuarioCriacao;
}