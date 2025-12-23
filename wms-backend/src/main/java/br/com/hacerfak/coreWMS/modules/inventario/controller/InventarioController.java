package br.com.hacerfak.coreWMS.modules.inventario.controller;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.inventario.domain.Inventario;
import br.com.hacerfak.coreWMS.modules.inventario.domain.TarefaContagem;
import br.com.hacerfak.coreWMS.modules.inventario.dto.ContagemRequest;
import br.com.hacerfak.coreWMS.modules.inventario.dto.InventarioRequest;
import br.com.hacerfak.coreWMS.modules.inventario.repository.TarefaContagemRepository;
import br.com.hacerfak.coreWMS.modules.inventario.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventarios")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;
    private final TarefaContagemRepository tarefaRepository;

    // --- GESTÃO ---
    @PostMapping
    @PreAuthorize("hasAuthority('INVENTARIO_CRIAR') or hasRole('ADMIN')")
    public ResponseEntity<Inventario> criar(@RequestBody @Valid InventarioRequest dto) {
        return ResponseEntity.ok(inventarioService.criarInventario(dto));
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAuthority('INVENTARIO_APROVAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> finalizar(@PathVariable Long id, Authentication auth) {
        inventarioService.finalizarInventario(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    // --- COLETOR ---
    @GetMapping("/tarefas")
    @PreAuthorize("hasAuthority('INVENTARIO_CONTAR') or hasRole('ADMIN')")
    public ResponseEntity<List<TarefaContagem>> listarTarefasPendentes() {
        return ResponseEntity.ok(tarefaRepository.findByStatus(StatusTarefa.PENDENTE));
    }

    @PostMapping("/tarefas/{id}/contar")
    @PreAuthorize("hasAuthority('INVENTARIO_CONTAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> registrarContagem(
            @PathVariable Long id,
            @RequestBody @Valid ContagemRequest dto,
            Authentication auth) {

        inventarioService.registrarContagem(id, dto.quantidade(), auth.getName());
        return ResponseEntity.ok().build();
    }
}