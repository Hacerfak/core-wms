package br.com.hacerfak.coreWMS.modules.cadastro.domain;

import jakarta.persistence.*;
import lombok.*;
import br.com.hacerfak.coreWMS.core.domain.BaseEntity;

@Entity
@Table(name = "tb_parceiro", indexes = {
        @Index(name = "idx_parceiro_documento", columnList = "documento", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Parceiro extends BaseEntity {

    @Column(nullable = false, length = 20) // CNPJ ou CPF (apenas números)
    private String documento;

    @Column(nullable = false)
    private String nome; // Razão Social

    @Column(length = 20)
    private String ie; // Inscrição Estadual (Importante para Fiscal)

    private String nomeFantasia;

    // Endereço
    private String cep;
    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    private String uf;
    private String telefone;
}
