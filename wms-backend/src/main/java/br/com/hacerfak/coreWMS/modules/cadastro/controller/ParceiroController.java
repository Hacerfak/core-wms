package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/parceiros")
@RequiredArgsConstructor
public class ParceiroController {

    private final ParceiroRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('PARCEIRO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Parceiro>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PARCEIRO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Parceiro> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PARCEIRO_CRIAR') or hasRole('ADMIN')")
    public ResponseEntity<Parceiro> criar(@RequestBody Parceiro parceiro) {
        return ResponseEntity.ok(repository.save(parceiro));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PARCEIRO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Parceiro> atualizar(@PathVariable Long id, @RequestBody Parceiro dados) {
        return repository.findById(id).map(parceiro -> {
            parceiro.setNome(dados.getNome());
            parceiro.setIe(dados.getIe());
            // Documento (CNPJ) geralmente não se muda
            return ResponseEntity.ok(repository.save(parceiro));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PARCEIRO_EXCLUIR') or hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!repository.existsById(id))
            return ResponseEntity.notFound().build();
        try {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            // Se tiver produtos vinculados, dá erro 400
            return ResponseEntity.badRequest().build();
        }
    }
}
