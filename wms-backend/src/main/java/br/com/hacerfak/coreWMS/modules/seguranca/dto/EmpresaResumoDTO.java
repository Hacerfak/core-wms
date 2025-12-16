package br.com.hacerfak.coreWMS.modules.seguranca.dto;

public record EmpresaResumoDTO(
        Long id,
        String razaoSocial,
        String tenantId,
        String role // O cargo dele nessa empresa (ADMIN, OPERADOR)
) {
}