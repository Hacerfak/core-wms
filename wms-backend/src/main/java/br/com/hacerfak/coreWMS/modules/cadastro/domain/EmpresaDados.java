package br.com.hacerfak.coreWMS.modules.cadastro.domain;

import br.com.hacerfak.coreWMS.core.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_empresa_dados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaDados extends BaseEntity {

    // ID herdado

    // --- DADOS CADASTRAIS ---
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private String inscricaoEstadual;
    private String inscricaoMunicipal;
    private String cnaePrincipal;
    private String regimeTributario;

    // --- CONTATO ---
    private String email;
    private String telefone;
    private String website;

    // --- ENDEREÇO ---
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;

    // --- CERTIFICADO DIGITAL ---
    @Column(name = "certificado_arquivo")
    private byte[] certificadoArquivo;

    @Column(name = "certificado_senha")
    private String certificadoSenha;

    private String nomeCertificado;
    private LocalDateTime validadeCertificado;
}