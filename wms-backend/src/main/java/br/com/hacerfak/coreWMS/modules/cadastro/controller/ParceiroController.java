package br.com.hacerfak.coreWMS.modules.cadastro.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parceiros")
@RequiredArgsConstructor
public class ParceiroController {

    private final ParceiroRepository repository;

    @GetMapping
    public ResponseEntity<List<Parceiro>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Parceiro> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Parceiro> criar(@RequestBody Parceiro parceiro) {
        return ResponseEntity.ok(repository.save(parceiro));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Parceiro> atualizar(@PathVariable Long id, @RequestBody Parceiro dados) {
        return repository.findById(id).map(parceiro -> {
            parceiro.setNome(dados.getNome());
            parceiro.setIe(dados.getIe());
            // Documento (CNPJ) geralmente não se muda
            return ResponseEntity.ok(repository.save(parceiro));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
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
