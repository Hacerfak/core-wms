package br.com.hacerfak.coreWMS.modules.cadastro.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tb_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa extends BaseEntity {

    @Column(nullable = false)
    private String razaoSocial;

    @Column(nullable = false, unique = true)
    private String cnpj;

    // Identificador interno do banco (ex: "tenant_00191")
    @Column(nullable = false, unique = true, name = "tenant_id")
    private String tenantId;

    // Metadados do Certificado (Preparamos o terreno para o onboarding)
    private String nomeCertificado;
    private LocalDate validadeCertificado;

    @Builder.Default
    private boolean ativo = true;
}