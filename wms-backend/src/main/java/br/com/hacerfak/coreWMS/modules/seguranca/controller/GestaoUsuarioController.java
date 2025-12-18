package br.com.hacerfak.coreWMS.modules.seguranca.controller;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.Perfil;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.CriarUsuarioRequest;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.UsuarioDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.repository.PerfilRepository;
import br.com.hacerfak.coreWMS.modules.seguranca.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gestao-usuarios")
@RequiredArgsConstructor
public class GestaoUsuarioController {

    private final UsuarioService usuarioService;
    private final PerfilRepository perfilRepository;

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarPorTenantAtual());
    }

    @PostMapping
    public ResponseEntity<Void> criarUsuario(@RequestBody CriarUsuarioRequest request) {
        usuarioService.criarUsuarioParaEmpresa(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/perfis")
    public ResponseEntity<List<Perfil>> listarPerfis() {
        return ResponseEntity.ok(perfilRepository.findAll());
    }
}