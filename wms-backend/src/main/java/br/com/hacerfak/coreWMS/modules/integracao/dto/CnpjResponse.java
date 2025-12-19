package br.com.hacerfak.coreWMS.modules.integracao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CnpjResponse {
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String situacao; // ATIVA, BAIXADA, NULA
    private String ie; // Inscrição Estadual
    private String cnaePrincipal;
    private String regimeTributario; // Simples Nacional, Lucro Presumido, Lucro Real

    // Endereço
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cep;
    private String cidade;
    private String uf;
}