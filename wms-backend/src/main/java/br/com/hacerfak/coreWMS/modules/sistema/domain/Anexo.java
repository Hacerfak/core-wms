package br.com.hacerfak.coreWMS.modules.sistema.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tb_anexo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Anexo extends BaseEntity {

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(nullable = false)
    private String contentType; // image/jpeg, application/pdf

    @Column(nullable = false)
    private String caminhoUrl; // URL do S3 ou Path local

    // Polimorfismo simples: Linkar com ID e Tipo de entidade
    @Column(name = "entidade_id", nullable = false)
    private Long entidadeId;

    @Column(name = "entidade_tipo", nullable = false)
    private String entidadeTipo; // "TAREFA_DIVERGENCIA", "SOLICITACAO_ENTRADA"

    private String descricao; // Ex: "Foto da caixa amassada"
}