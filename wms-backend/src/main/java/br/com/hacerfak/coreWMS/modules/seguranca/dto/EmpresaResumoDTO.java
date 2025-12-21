package br.com.hacerfak.coreWMS.modules.seguranca.dto;

public record EmpresaResumoDTO(
                Long id,
                String razaoSocial,
                String cnpj, // Pode ser usado para passar o CNPJ ou TenantId dependendo da tela
                String tenantId,
                String perfil // Ex: "ADMIN", "OPERADOR" ou "ATIVO" (na listagem geral)
) {
}