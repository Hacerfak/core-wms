package br.com.hacerfak.coreWMS.modules.cadastro.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    // Dados Cadastrais
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private String inscricaoEstadual;
    private String inscricaoMunicipal;
    private String cnaePrincipal;
    private String regimeTributario;

    // Contato
    private String email;
    private String telefone;
    private String website;

    // Endereço Estruturado
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf; // <--- ESTE É O CAMPO QUE MANDA NA BUSCA SEFAZ

    @Deprecated
    private String enderecoCompleto;

    // CERTIFICADO DIGITAL
    @Column(name = "certificado_arquivo")
    private byte[] certificadoArquivo;

    @Column(name = "certificado_senha")
    private String certificadoSenha;

    // METADADOS (Essenciais para o front saber se tem certificado)
    private String nomeCertificado;
    private LocalDateTime validadeCertificado;

    // Configs de Regra
    private boolean permiteEstoqueNegativo;
    private boolean recebimentoCegoObrigatorio;
}