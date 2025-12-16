package br.com.hacerfak.coreWMS.modules.seguranca.dto;

import java.util.List;

public record LoginResponseDTO(
                String nome,
                String token, // Token "Global" ou temporário
                List<EmpresaResumoDTO> acessos) {
}