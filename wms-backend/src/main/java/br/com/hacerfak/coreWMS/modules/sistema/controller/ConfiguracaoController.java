package br.com.hacerfak.coreWMS.modules.sistema.controller;

import br.com.hacerfak.coreWMS.modules.sistema.domain.Configuracao;
import br.com.hacerfak.coreWMS.modules.sistema.repository.ConfiguracaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoController {

    private final ConfiguracaoRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public List<Configuracao> listar() {
        return repository.findAll();
    }

    // Endpoint rápido para pegar o valor booleano
    @GetMapping("/recebimento-exibir-qtd")
    @PreAuthorize("hasAuthority('RECEBIMENTO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> deveExibirQtd() {
        return repository.findById("RECEBIMENTO_EXIBIR_QTD_ESPERADA")
                .map(conf -> ResponseEntity.ok(Boolean.parseBoolean(conf.getValor())))
                .orElse(ResponseEntity.ok(true)); // Padrão é true
    }

    @PutMapping("/{chave}")
    @PreAuthorize("hasAuthority('CONFIG_GERENCIAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> atualizar(@PathVariable String chave, @RequestBody Map<String, String> body) {
        return repository.findById(chave)
                .map(conf -> {
                    conf.setValor(body.get("valor"));
                    repository.save(conf);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}