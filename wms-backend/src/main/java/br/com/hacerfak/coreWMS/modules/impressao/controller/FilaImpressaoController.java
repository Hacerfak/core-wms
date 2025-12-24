package br.com.hacerfak.coreWMS.modules.impressao.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.impressao.domain.FilaImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.domain.StatusImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.dto.PrintJobDTO;
import br.com.hacerfak.coreWMS.modules.impressao.repository.FilaImpressaoRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.ImpressaoService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;

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

    /**
     * Endpoint otimizado: O Agente chama isso a cada 1s ou 500ms.
     * Custo no Banco: ZERO.
     * Custo no Redis: Baixíssimo (O(1)).
     */
    @GetMapping("/jobs/poll")
    public ResponseEntity<List<PrintJobDTO>> buscarNovosTrabalhos(@RequestHeader("X-Agent-Key") String apiKey) {
        // 1. Verifica autenticação (Cachear isso no Redis também seria ótimo!)

        // 2. Busca o próximo Job ID no Redis
        String redisKey = "wms:print:jobs:pending";
        // Pop remove o item da lista e retorna. Garante que só 1 agente pegue (se tiver
        // múltiplos).
        String jobId = redisTemplate.opsForList().leftPop(redisKey);

        if (jobId == null) {
            return ResponseEntity.ok(Collections.emptyList()); // Nada para fazer
        }

        // 3. Se achou um ID no Redis, aí sim vai no Banco pegar os detalhes pesados
        // (ZPL)
        return filaRepository.findById(Long.valueOf(jobId))
                .map(job -> {
                    // Marca como processando para não pegar de novo se o Redis falhar
                    job.setStatus(StatusImpressao.EM_PROCESSAMENTO);
                    filaRepository.save(job);

                    PrintJobDTO dto = PrintJobDTO.builder()
                            .id(job.getId())
                            .zpl(job.getZplConteudo())
                            .ip(job.getImpressoraAlvo().getEnderecoIp())
                            .porta(job.getImpressoraAlvo().getPorta())
                            .build();
                    return ResponseEntity.ok(List.of(dto));
                })
                .orElseGet(() -> ResponseEntity.ok(Collections.emptyList()));
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