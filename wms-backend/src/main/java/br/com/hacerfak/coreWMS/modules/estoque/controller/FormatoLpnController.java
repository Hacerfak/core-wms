package br.com.hacerfak.coreWMS.modules.estoque.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.FormatoLpn;
import br.com.hacerfak.coreWMS.modules.estoque.dto.FormatoLpnRequest;
import br.com.hacerfak.coreWMS.modules.estoque.service.FormatoLpnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque/formatos-lpn")
@RequiredArgsConstructor
public class FormatoLpnController {

    private final FormatoLpnService service;

    /**
     * Lista todos os formatos (Ativos e Inativos) para a grid de cadastro.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ESTOQUE_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<FormatoLpn>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    /**
     * Lista apenas os ativos para o Modal de Seleção na Operação.
     */
    @GetMapping("/ativos")
    @PreAuthorize("hasAuthority('ESTOQUE_OPERAR') or hasRole('ADMIN')")
    public ResponseEntity<List<FormatoLpn>> listarAtivos() {
        return ResponseEntity.ok(service.listarAtivos());
    }

    /**
     * Busca um formato específico por ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ESTOQUE_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<FormatoLpn> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cria ou Atualiza um formato.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<FormatoLpn> salvar(@RequestBody @Valid FormatoLpnRequest dto) {
        FormatoLpn salvo = service.salvar(dto.toEntity());
        return ResponseEntity.ok(salvo);
    }

    /**
     * Alterna o status (Ativo/Inativo).
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> alternarStatus(@PathVariable Long id) {
        service.alternarStatus(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Exclui um formato (se não estiver em uso).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.ok().build();
    }
}