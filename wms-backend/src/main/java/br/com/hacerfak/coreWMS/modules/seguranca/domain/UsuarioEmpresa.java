// Arquivo: wms-backend/src/main/java/br/com/hacerfak/coreWMS/modules/seguranca/domain/UsuarioEmpresa.java

package br.com.hacerfak.coreWMS.modules.seguranca.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
// CORREÇÃO: Adicionado schema = "public" para evitar busca no tenant errado
@Table(name = "tb_usuario_empresa", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // <--- MUDANÇA: Use SuperBuilder
public class UsuarioEmpresa extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnore
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
}