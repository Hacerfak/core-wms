package br.com.hacerfak.coreWMS.modules.seguranca.controller;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.Perfil;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.CriarUsuarioRequest;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.EmpresaResumoDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.dto.UsuarioDTO;
import br.com.hacerfak.coreWMS.modules.seguranca.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @PreAuthorize("hasAuthority('USUARIO_LISTAR') or hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodosGlobal());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_LISTAR') or hasRole('ADMIN')")
    public ResponseEntity<UsuarioDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIO_CRIAR') or hasRole('ADMIN')")
    public ResponseEntity<UsuarioDTO> criarUsuario(@RequestBody CriarUsuarioRequest request) {
        UsuarioDTO criado = usuarioService.salvarUsuarioGlobal(null, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<UsuarioDTO> atualizarUsuario(@PathVariable Long id,
            @RequestBody CriarUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.salvarUsuarioGlobal(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluirUsuario(@PathVariable Long id) {
        usuarioService.excluirUsuarioGlobal(id);
        return ResponseEntity.noContent().build();
    }

    // --- VÍNCULOS ---

    @GetMapping("/{id}/empresas")
    @PreAuthorize("hasAuthority('USUARIO_LISTAR') or hasRole('ADMIN')")
    public ResponseEntity<List<EmpresaResumoDTO>> listarEmpresasVinculadas(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.listarEmpresasDoUsuario(id));
    }

    // NOVO: Endpoint para listar perfis disponíveis na empresa selecionada
    @GetMapping("/perfis-disponiveis/{empresaId}")
    @PreAuthorize("hasAuthority('USUARIO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Perfil>> listarPerfisDaEmpresa(@PathVariable Long empresaId) {
        return ResponseEntity.ok(usuarioService.listarPerfisPorEmpresa(empresaId));
    }

    // CORREÇÃO: Recebe perfilId como Long (não mais role string)
    @PostMapping("/{id}/empresas")
    @PreAuthorize("hasAuthority('USUARIO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> vincularEmpresa(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Long empresaId = Long.valueOf(body.get("empresaId").toString());
        Long perfilId = Long.valueOf(body.get("perfilId").toString()); // <--- CORREÇÃO AQUI

        usuarioService.vincularEmpresa(id, empresaId, perfilId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/empresas/{empresaId}")
    @PreAuthorize("hasAuthority('USUARIO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> desvincularEmpresa(@PathVariable Long id, @PathVariable Long empresaId) {
        usuarioService.desvincularEmpresa(id, empresaId);
        return ResponseEntity.noContent().build();
    }
}