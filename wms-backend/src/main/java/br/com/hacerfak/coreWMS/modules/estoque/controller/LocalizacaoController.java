package br.com.hacerfak.coreWMS.modules.estoque.controller;

import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LocalizacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locais")
@RequiredArgsConstructor
public class LocalizacaoController {

    private final LocalizacaoRepository repository;

    @GetMapping
    public ResponseEntity<List<Localizacao>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Localizacao> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Localizacao> criar(@RequestBody Localizacao local) {
        return ResponseEntity.ok(repository.save(local));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Localizacao> atualizar(@PathVariable Long id, @RequestBody Localizacao dados) {
        return repository.findById(id).map(local -> {
            local.setTipo(dados.getTipo());
            local.setBloqueado(dados.isBloqueado());
            local.setAtivo(dados.isAtivo());
            local.setCapacidadePesoKg(dados.getCapacidadePesoKg());
            return ResponseEntity.ok(repository.save(local));
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
            return ResponseEntity.badRequest().build(); // Não deleta se tiver saldo
        }
    }
}
