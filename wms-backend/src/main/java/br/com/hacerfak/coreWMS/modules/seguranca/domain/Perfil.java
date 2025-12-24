package br.com.hacerfak.coreWMS.modules.seguranca.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder; // Importante

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // <--- MUDANÇA: Use SuperBuilder
public class Perfil extends BaseEntity {

    @Column(nullable = false)
    private String nome;
    private String descricao;

    @Builder.Default
    private boolean ativo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_perfil_permissoes", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "permissao")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<PermissaoEnum> permissoes = new HashSet<>();
}