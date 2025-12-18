package br.com.hacerfak.coreWMS.modules.seguranca.controller;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.Perfil;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.CriarUsuarioRequest;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.UsuarioDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.VerificarUsuarioDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.PerfilRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/gestao-usuarios")
@RequiredArgsConstructor
public class GestaoUsuarioController {

    private final UsuarioService usuarioService;
    private final PerfilRepository perfilRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIO_LISTAR') or hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarPorTenantAtual());
    }

    // Endpoint para o Front checar se o login já existe antes de pedir senha
    @GetMapping("/verificar/{login}")
    @PreAuthorize("hasAuthority('USUARIO_CRIAR') or hasRole('ADMIN')")
    public ResponseEntity<VerificarUsuarioDTO> verificarUsuario(@PathVariable String login) {
        return ResponseEntity.ok(usuarioService.verificarExistencia(login));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIO_CRIAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> salvarUsuario(@RequestBody CriarUsuarioRequest request) {
        usuarioService.salvarUsuarioParaEmpresa(request); // Método renomeado
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/perfis")
    @PreAuthorize("hasAuthority('USUARIO_LISTAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Perfil>> listarPerfis() {
        return ResponseEntity.ok(perfilRepository.findAll());
    }
}