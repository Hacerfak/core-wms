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
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/impressao/fila")
@RequiredArgsConstructor
public class FilaImpressaoController {

    private final ImpressaoService impressaoService;
    private final FilaImpressaoRepository filaRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_QUEUE_KEY = "wms:print:jobs:pending";

    // --- MÉTODOS PARA O AGENTE ---

    /**
     * Endpoint de Long Polling STATELESS e ESCALÁVEL.
     * O Agente chama isso. A thread do Java fica "presa" aqui esperando o Redis
     * (BLPOP),
     * mas como usamos Redis, funciona em cluster.
     * Se nada chegar em 20 segundos, retorna vazio (Heartbeat).
     */
    @GetMapping("/poll")
    public ResponseEntity<List<PrintJobDTO>> aguardarTrabalho(@RequestParam String agentId) {
        // Bloqueia e espera por um item na lista do Redis por até 20 segundos
        // Requer Spring Data Redis 2.6+ (SpringBoot 3/4 suportam nativamente)
        String jobId = redisTemplate.opsForList().leftPop(REDIS_QUEUE_KEY, Duration.ofSeconds(20));

        if (jobId == null) {
            // Timeout: nenhum job chegou
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Se pegou um Job, busca os detalhes no banco
        return filaRepository.findById(Long.valueOf(jobId))
                .map(job -> {
                    job.setStatus(StatusImpressao.EM_PROCESSAMENTO);
                    filaRepository.save(job);

                    PrintJobDTO dto = PrintJobDTO.builder()
                            .id(job.getId())
                            .zpl(job.getZplConteudo())
                            .tipoConexao(job.getImpressoraAlvo().getTipoConexao().name())
                            .ip(job.getImpressoraAlvo().getEnderecoIp())
                            .porta(job.getImpressoraAlvo().getPorta())
                            .caminhoCompartilhamento(job.getImpressoraAlvo().getCaminhoCompartilhamento())
                            .build();
                    return ResponseEntity.ok(List.of(dto));
                })
                .orElseGet(() -> ResponseEntity.ok(Collections.emptyList()));
    }

    // --- Endpoint simplificado que o Agente já usava (mantido para
    // compatibilidade, agora apontando para a lógica nova) ---
    @GetMapping("/jobs/poll")
    public ResponseEntity<List<PrintJobDTO>> buscarNovosTrabalhos(@RequestHeader("X-Agent-Key") String apiKey) {
        // Reutiliza a lógica, mas com timeout menor se desejar resposta rápida
        String jobId = redisTemplate.opsForList().leftPop(REDIS_QUEUE_KEY, Duration.ofSeconds(1));

        if (jobId == null)
            return ResponseEntity.ok(Collections.emptyList());

        return filaRepository.findById(Long.valueOf(jobId))
                .map(job -> {
                    job.setStatus(StatusImpressao.EM_PROCESSAMENTO);
                    filaRepository.save(job);
                    // ... mapeamento DTO ...
                    PrintJobDTO dto = PrintJobDTO.builder()
                            .id(job.getId())
                            .zpl(job.getZplConteudo())
                            .ip(job.getImpressoraAlvo().getEnderecoIp())
                            .porta(job.getImpressoraAlvo().getPorta())
                            .caminhoCompartilhamento(job.getImpressoraAlvo().getCaminhoCompartilhamento())
                            .build();
                    return ResponseEntity.ok(List.of(dto));
                })
                .orElseGet(() -> ResponseEntity.ok(Collections.emptyList()));
    }

    // Endpoint de conclusão e erro mantidos...
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