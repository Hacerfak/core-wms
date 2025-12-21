package br.com.hacerfak.coreWMS.modules.operacao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tb_volume_recebimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolumeRecebimento extends BaseEntity {

    // ID herdado

    @ManyToOne
    @JoinColumn(name = "recebimento_id", nullable = false)
    private Recebimento recebimento;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, unique = true)
    private String lpn;

    @Column(nullable = false)
    private BigDecimal quantidadeOriginal;

    @Builder.Default
    private boolean armazenado = false;

    @ManyToOne
    @JoinColumn(name = "local_destino_id")
    private Localizacao localDestino;

    // Data criação e Usuário criação agora vêm da BaseEntity
    private String usuarioCriacao; // Mantido para compatibilidade do código existente
}