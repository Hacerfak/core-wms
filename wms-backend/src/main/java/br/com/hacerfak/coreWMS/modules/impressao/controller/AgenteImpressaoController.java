package br.com.hacerfak.coreWMS.modules.impressao.controller;

import br.com.hacerfak.coreWMS.modules.impressao.domain.AgenteImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.dto.AgenteRequest;
import br.com.hacerfak.coreWMS.modules.impressao.dto.AgenteResponse;
import br.com.hacerfak.coreWMS.modules.impressao.repository.AgenteImpressaoRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.AgenteImpressaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/impressao/agentes")
@RequiredArgsConstructor
public class AgenteImpressaoController {

    private final AgenteImpressaoService service;
    private final AgenteImpressaoRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<List<AgenteResponse>> listar() {
        List<AgenteResponse> lista = repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<AgenteResponse> criar(@RequestBody @Valid AgenteRequest request) {
        AgenteImpressao agente = service.criarAgente(request);
        return ResponseEntity.ok(toResponse(agente));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<Void> revogar(@PathVariable Long id) {
        service.revogarAcesso(id);
        return ResponseEntity.noContent().build();
    }

    private AgenteResponse toResponse(AgenteImpressao a) {
        boolean isOnline = a.getUltimoHeartbeat() != null &&
                a.getUltimoHeartbeat().isAfter(LocalDateTime.now().minusSeconds(60));

        return new AgenteResponse(
                a.getId(),
                a.getNome(),
                a.getDescricao(),
                a.getHostname(),
                a.getApiKey(), // Em produção real, talvez ocultar parte da chave
                a.isAtivo(),
                a.getUltimoHeartbeat(),
                a.getVersaoAgente(),
                isOnline ? "ONLINE" : "OFFLINE");
    }
}