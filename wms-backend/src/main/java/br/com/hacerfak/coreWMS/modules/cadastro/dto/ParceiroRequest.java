package br.com.hacerfak.coreWMS.modules.cadastro.dto;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

// Record do Java (DTO Imutável)
public record ParceiroRequest(
        String id, // Opcional no create, usado no update

        @NotBlank(message = "Documento é obrigatório") String documento,

        @NotBlank(message = "Razão Social é obrigatória") String nome,

        String nomeFantasia,
        String ie,
        String crt,
        String tipo,

        Boolean ativo,
        Boolean recebimentoCego,
        Boolean padraoControlaLote,
        Boolean padraoControlaValidade,
        Boolean padraoControlaSerie,

        // Endereço Plano
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,

        String telefone,
        String email) implements Serializable {
}