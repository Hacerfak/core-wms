package br.com.hacerfak.coreWMS.modules.sistema.controller;

import br.com.hacerfak.coreWMS.modules.sistema.domain.Configuracao;
import br.com.hacerfak.coreWMS.modules.sistema.repository.ConfiguracaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoController {

    private final ConfiguracaoRepository repository;

    @GetMapping
    public List<Configuracao> listar() {
        return repository.findAll();
    }

    // Endpoint rápido para pegar o valor booleano
    @GetMapping("/recebimento-exibir-qtd")
    public ResponseEntity<Boolean> deveExibirQtd() {
        return repository.findById("RECEBIMENTO_EXIBIR_QTD_ESPERADA")
                .map(conf -> ResponseEntity.ok(Boolean.parseBoolean(conf.getValor())))
                .orElse(ResponseEntity.ok(true)); // Padrão é true
    }

    @PutMapping("/{chave}")
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