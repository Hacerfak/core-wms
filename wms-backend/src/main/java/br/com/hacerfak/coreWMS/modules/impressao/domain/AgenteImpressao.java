package br.com.hacerfak.coreWMS.modules.impressao.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_agente_impressao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AgenteImpressao extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String nome;

    private String descricao;
    private String hostname;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @Builder.Default
    private boolean ativo = true;

    private LocalDateTime ultimoHeartbeat;
    private String versaoAgente;

    public void registrarAtividade(String versao) {
        this.ultimoHeartbeat = LocalDateTime.now();
        if (versao != null)
            this.versaoAgente = versao;
    }
}