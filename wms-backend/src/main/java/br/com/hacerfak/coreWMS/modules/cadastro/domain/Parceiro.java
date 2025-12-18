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

    @Column(nullable = false, length = 20)
    private String documento; // CNPJ/CPF

    @Column(nullable = false)
    private String nome;

    @Column(length = 20)
    private String ie;

    private String nomeFantasia;

    @Column(length = 5)
    private String crt;

    // --- NOVOS CAMPOS ---

    @Builder.Default
    private boolean ativo = true;

    // Configurações / Parâmetros
    @Builder.Default
    private boolean recebimentoCego = false; // Se true, esconde a quantidade na conferência

    // Endereço
    private String cep;
    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    private String uf;
    private String telefone;
    private String email; // Adicionado para contato

    // Tipo (Fornecedor, Cliente, Ambos) - Opcional, mas bom para filtro
    private String tipo;
}