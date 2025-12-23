package br.com.hacerfak.coreWMS.modules.cadastro.domain;

import jakarta.persistence.*;
import lombok.*;
import br.com.hacerfak.coreWMS.core.domain.BaseEntity;

@Entity
@Table(name = "tb_parceiro", indexes = {
        @Index(name = "idx_parceiro_doc", columnList = "cpf_cnpj", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Parceiro extends BaseEntity {

    // CORREÇÃO: Renomeado de 'documento' para 'cpfCnpj' para bater com o
    // getCpfCnpj()
    @Column(name = "cpf_cnpj", nullable = false, length = 20, unique = true)
    private String cpfCnpj;

    @Column(nullable = false)
    private String nome;

    @Column(length = 20)
    private String ie;

    private String nomeFantasia;

    @Column(length = 5)
    private String crt;

    @Builder.Default
    private boolean ativo = true;

    // Configurações / Parâmetros
    @Builder.Default
    private boolean recebimentoCego = false;

    @Builder.Default
    private Boolean padraoControlaLote = false;
    @Builder.Default
    private Boolean padraoControlaValidade = false;
    @Builder.Default
    private Boolean padraoControlaSerie = false;

    // Endereço
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf; // Estado

    // Contato
    private String telefone;
    private String email;

    // Tipo (CLIENTE, FORNECEDOR, TRANSPORTADORA)
    private String tipo;

    // Método auxiliar caso algum código antigo ainda chame getDocumento
    public String getDocumento() {
        return this.cpfCnpj;
    }
}