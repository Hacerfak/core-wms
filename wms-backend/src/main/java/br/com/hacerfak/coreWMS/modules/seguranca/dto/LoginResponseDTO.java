package br.com.hacerfak.coreWMS.modules.seguranca.dto;

import java.util.List;

public record LoginResponseDTO(
                String token,
                Long id,
                String usuario,
                String nome,
                String role,
                List<EmpresaResumoDTO> empresas) {
}