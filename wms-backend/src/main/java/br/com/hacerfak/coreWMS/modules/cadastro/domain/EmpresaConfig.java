package br.com.hacerfak.coreWMS.modules.cadastro.domain;

import jakarta.persistence.*;
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
    private Long id;

    private String razaoSocial;
    private String cnpj;
    private String uf;
    private String enderecoCompleto;

    // Armazenamos o PFX como array de bytes
    @Column(name = "certificado_arquivo")
    private byte[] certificadoArquivo;

    @Column(name = "certificado_senha")
    private String certificadoSenha;

    // Configs
    private boolean permiteEstoqueNegativo;
    private boolean recebimentoCegoObrigatorio;
}