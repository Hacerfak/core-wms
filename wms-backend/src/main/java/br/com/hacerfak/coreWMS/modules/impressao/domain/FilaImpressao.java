package br.com.hacerfak.coreWMS.modules.impressao.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_fila_impressao", indexes = {
        @Index(name = "idx_fila_status_imp", columnList = "status, impressora_alvo_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilaImpressao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    private LocalDateTime dataCriacao = LocalDateTime.now();
    private LocalDateTime dataAtualizacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusImpressao status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String zplConteudo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "impressora_alvo_id", nullable = false)
    private Impressora impressoraAlvo;

    @Builder.Default
    private Integer tentativas = 0;

    @Column(length = 500)
    private String mensagemErro;

    private String usuarioSolicitante;
    private String origem; // "APP_ANDROID", "WEB_EXPEDICAO"

    @PreUpdate
    public void preUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }
}