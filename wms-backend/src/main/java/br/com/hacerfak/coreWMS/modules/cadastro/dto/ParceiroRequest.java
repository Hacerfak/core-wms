package br.com.hacerfak.coreWMS.modules.cadastro.dto;

import jakarta.validation.constraints.NotBlank;

public record ParceiroRequest(
                @NotBlank(message = "O nome é obrigatório") String nome,

                @NotBlank(message = "O documento (CNPJ/CPF) é obrigatório") String documento,

                String ie,
                String nomeFantasia,
                String crt,

                // Configurações
                Boolean ativo,
                Boolean recebimentoCego,

                // Endereço
                String cep,
                String logradouro,
                String numero,
                String bairro,
                String cidade,
                String uf,

                // Contato
                String telefone,
                String email,
                String tipo // FORNECEDOR, CLIENTE, AMBOS
) {
}