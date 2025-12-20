package br.com.hacerfak.coreWMS.modules.sistema.controller;

import br.com.hacerfak.coreWMS.modules.sistema.domain.SistemaConfig;
import br.com.hacerfak.coreWMS.modules.sistema.repository.SistemaConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sistema-config")
@RequiredArgsConstructor
public class SistemaConfigController {

    private final SistemaConfigRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public List<SistemaConfig> listar() {
        return repository.findAll();
    }

    @GetMapping("/valor/{chave}")
    public ResponseEntity<String> getValor(@PathVariable String chave) {
        return repository.findById(chave).map(c -> ResponseEntity.ok(c.getValor()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{chave}")
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> update(@PathVariable String chave, @RequestBody Map<String, String> body) {
        return repository.findById(chave).map(c -> {
            c.setValor(body.get("valor"));
            repository.save(c);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}