// Arquivo: wms-backend/src/main/java/br/com/hacerfak/coreWMS/modules/cadastro/domain/Empresa.java

package br.com.hacerfak.coreWMS.modules.cadastro.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
// CORREÇÃO: Adicionado schema = "public"
@Table(name = "tb_empresa", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // <--- MUDANÇA: Use SuperBuilder
public class Empresa extends BaseEntity {

    @Column(nullable = false)
    private String razaoSocial;

    @Column(nullable = false, unique = true)
    private String cnpj;

    @Column(nullable = false, unique = true, name = "tenant_id")
    private String tenantId;

    private String nomeCertificado;
    private java.time.LocalDate validadeCertificado;

    @Builder.Default
    private boolean ativo = true;
}