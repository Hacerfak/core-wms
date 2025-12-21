package br.com.hacerfak.coreWMS.modules.seguranca.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_usuario_perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioPerfil extends BaseEntity {

    // ID herdado

    @Column(nullable = false)
    private Long usuarioId;

    @ManyToOne
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;
}