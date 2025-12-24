package br.com.hacerfak.coreWMS.modules.seguranca.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder; // Importante

@Entity
@Table(name = "tb_usuario_perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // <--- MUDANÇA: Use SuperBuilder
public class UsuarioPerfil extends BaseEntity {

    // O ID é herdado de BaseEntity e mapeado para a coluna 'id' criada no SQL acima

    @Column(nullable = false)
    private Long usuarioId;

    @ManyToOne
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;
}