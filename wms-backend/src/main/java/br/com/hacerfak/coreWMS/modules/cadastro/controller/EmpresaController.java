package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.seguranca.dto.EmpresaResumoDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/meus-acessos")
    public ResponseEntity<List<EmpresaResumoDTO>> listarMinhasEmpresas() {
        // Pega o usuário logado pelo Token
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByLogin(login).orElseThrow();

        // Converte a lista de entidades para DTO
        List<EmpresaResumoDTO> lista = usuario.getAcessos().stream()
                .map(acesso -> new EmpresaResumoDTO(
                        acesso.getEmpresa().getId(),
                        acesso.getEmpresa().getRazaoSocial(),
                        acesso.getEmpresa().getTenantId(),
                        acesso.getRole().name()))
                .toList();

        return ResponseEntity.ok(lista);
    }
}