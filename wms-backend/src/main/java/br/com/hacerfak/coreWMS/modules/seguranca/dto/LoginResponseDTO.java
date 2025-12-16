package br.com.hacerfak.coreWMS.modules.seguranca.dto;

import java.util.List;

public record LoginResponseDTO(
        String token,
        String usuario,
        List<EmpresaResumoDTO> empresas) {
}