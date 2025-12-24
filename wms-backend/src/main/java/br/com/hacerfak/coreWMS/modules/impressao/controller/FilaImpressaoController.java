package br.com.hacerfak.coreWMS.modules.impressao.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.impressao.domain.FilaImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.dto.PrintJobDTO;
import br.com.hacerfak.coreWMS.modules.impressao.repository.FilaImpressaoRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.ImpressaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.context.request.async.DeferredResult;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/impressao/fila")
@RequiredArgsConstructor
public class FilaImpressaoController {

    private final ImpressaoService impressaoService;
    private final FilaImpressaoRepository filaRepository;
    private final Map<String, DeferredResult<List<PrintJobDTO>>> pollingClients = new ConcurrentHashMap<>();

    // --- MÉTODOS PARA O AGENTE ---

    /**
     * O Agente chama isso a cada N segundos para pegar trabalho.
     * Idealmente, protegido por uma API Key ou Token de Serviço.
     */
    @GetMapping("/pendentes")
    public ResponseEntity<List<PrintJobDTO>> buscarPendentes() {
        return ResponseEntity.ok(impressaoService.buscarTrabalhosPendentes());
    }

    @PostMapping("/{id}/concluir")
    public ResponseEntity<Void> confirmarSucesso(@PathVariable Long id) {
        impressaoService.atualizarStatusFila(id, true, null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/erro")
    public ResponseEntity<Void> reportarErro(@PathVariable Long id, @RequestBody String erro) {
        impressaoService.atualizarStatusFila(id, false, erro);
        return ResponseEntity.ok().build();
    }

    // O Agente chama este endpoint
    @GetMapping("/poll")
    public DeferredResult<List<PrintJobDTO>> aguardarTrabalho(@RequestParam String agentId) {
        // Timeout de 30s. Se ninguém imprimir nada, retorna lista vazia (204 No
        // Content)
        DeferredResult<List<PrintJobDTO>> output = new DeferredResult<>(30000L, Collections.emptyList());

        pollingClients.put(agentId, output);

        output.onCompletion(() -> pollingClients.remove(agentId));
        return output;
    }

    // No método que cria o job (ImpressaoService), você chama este método para
    // "acordar" o agente
    public void notificarAgente(String agentId, List<PrintJobDTO> jobs) {
        if (pollingClients.containsKey(agentId)) {
            pollingClients.get(agentId).setResult(jobs);
        }
    }

    // --- MÉTODOS DE DEBUG E CONSULTA ---

    @GetMapping("/{id}/debug-zpl")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> verZplGerado(@PathVariable Long id) {
        FilaImpressao fila = filaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job não encontrado"));

        // Retorna o ZPL puro para inspecionar no navegador
        return ResponseEntity.ok(fila.getZplConteudo());
    }
}