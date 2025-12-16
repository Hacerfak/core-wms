package br.com.hacerfak.coreWMS.modules.cadastro.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "tb_empresa_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaConfig {
    @Id
    private Long id; // Sempre 1

    private String razaoSocial;
    private String cnpj;
    private String enderecoCompleto;
    private String logoUrl;

    private boolean permiteEstoqueNegativo;
    private boolean recebimentoCegoObrigatorio;
}