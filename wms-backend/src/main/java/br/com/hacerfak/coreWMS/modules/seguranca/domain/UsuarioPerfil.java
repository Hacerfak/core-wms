package br.com.hacerfak.coreWMS.modules.seguranca.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_usuario_perfil")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioPerfil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId; // ID do Master

    @ManyToOne
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;
}