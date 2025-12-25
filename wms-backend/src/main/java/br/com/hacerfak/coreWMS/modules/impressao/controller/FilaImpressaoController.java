package br.com.hacerfak.coreWMS.modules.impressao.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.impressao.domain.FilaImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.domain.StatusImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.dto.PrintJobDTO;
import br.com.hacerfak.coreWMS.modules.impressao.repository.AgenteImpressaoRepository;
import br.com.hacerfak.coreWMS.modules.impressao.repository.FilaImpressaoRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.ImpressaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.web.PageableDefault;
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
    private final AgenteImpressaoRepository agenteRepository;

    // --- MÉTODOS PARA O FRONTEND (HUMANOS) ---

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<Page<FilaImpressao>> listarFila(
            @PageableDefault(size = 20, sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<FilaImpressao> page = filaRepository.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}/debug-zpl")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> verZplGerado(@PathVariable Long id) {
        FilaImpressao fila = filaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job não encontrado"));
        return ResponseEntity.ok(fila.getZplConteudo());
    }

    // --- MÉTODOS PARA O AGENTE (MÁQUINAS) ---

    @GetMapping("/poll")
    public ResponseEntity<List<PrintJobDTO>> aguardarTrabalho(
            @RequestParam(required = false) String agentId,
            @RequestHeader(value = "X-Agent-Key", required = false) String apiKey) {

        // 1. Validação de Segurança
        if (apiKey == null || !agenteRepository.findByApiKeyAndAtivoTrue(apiKey).isPresent()) {
            return ResponseEntity.status(403).build();
        }

        String tenantId = br.com.hacerfak.coreWMS.core.multitenant.TenantContext.getTenant();
        if (tenantId == null)
            tenantId = "wms_master";

        String redisKey = "wms:print:jobs:pending:" + tenantId;

        // 2. Lógica de "Batch Polling" (Drena a fila)

        // A. Espera o primeiro (Long Polling)
        String firstJobId = redisTemplate.opsForList().leftPop(redisKey, Duration.ofSeconds(20));

        if (firstJobId == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<String> jobIds = new java.util.ArrayList<>();
        jobIds.add(firstJobId);

        // B. Pega o restante imediatamente (até 50)
        while (jobIds.size() < 50) {
            String nextJobId = redisTemplate.opsForList().leftPop(redisKey);
            if (nextJobId == null) {
                break;
            }
            jobIds.add(nextJobId);
        }

        // 3. Processa e Monta DTOs
        List<PrintJobDTO> jobsParaEnviar = new java.util.ArrayList<>();

        for (String idStr : jobIds) {
            try {
                Long id = Long.valueOf(idStr);
                filaRepository.findById(id).ifPresent(job -> {
                    job.setStatus(StatusImpressao.EM_PROCESSAMENTO);
                    filaRepository.save(job);

                    jobsParaEnviar.add(PrintJobDTO.builder()
                            .id(job.getId())
                            .zpl(job.getZplConteudo())
                            .tipoConexao(job.getImpressoraAlvo().getTipoConexao().name())
                            .ip(job.getImpressoraAlvo().getEnderecoIp())
                            .porta(job.getImpressoraAlvo().getPorta())
                            .caminhoCompartilhamento(job.getImpressoraAlvo().getCaminhoCompartilhamento())
                            .build());
                });
            } catch (Exception e) {
                System.err.println("Erro ao processar job ID " + idStr + ": " + e.getMessage());
            }
        }

        return ResponseEntity.ok(jobsParaEnviar);
    }

    @PostMapping("/{id}/concluir")
    public ResponseEntity<Void> confirmarSucesso(@PathVariable Long id) {
        impressaoService.atualizarStatusFila(id, true, null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/erro")
    public ResponseEntity<Void> reportarErro(@PathVariable Long id, @RequestBody(required = false) String erro) {
        String msg = erro != null ? erro : "Erro desconhecido reportado pelo agente";
        impressaoService.atualizarStatusFila(id, false, msg);
        return ResponseEntity.ok().build();
    }
}