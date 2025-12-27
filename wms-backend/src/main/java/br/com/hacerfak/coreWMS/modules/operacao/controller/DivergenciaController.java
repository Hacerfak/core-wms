package br.com.hacerfak.coreWMS.modules.operacao.controller;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.operacao.domain.TarefaDivergencia;
import br.com.hacerfak.coreWMS.modules.operacao.repository.TarefaDivergenciaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.service.RecebimentoWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recebimentos/divergencias")
@RequiredArgsConstructor
public class DivergenciaController {

    private final TarefaDivergenciaRepository divergenciaRepository;
    private final RecebimentoWorkflowService workflowService;

    // Listar divergências pendentes (Status PENDENTE)
    @GetMapping
    @PreAuthorize("hasAuthority('RECEBIMENTO_FINALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<TarefaDivergencia>> listarPendentes() {
        // Você pode criar um método no repository: findByStatus(StatusTarefa.PENDENTE)
        // Ou filtrar no stream se for pouco volume, mas repository é melhor.
        // Assumindo que o repository padrão JpaRepository já permite findByStatus se
        // declarado.
        return ResponseEntity.ok(divergenciaRepository.findByStatus(StatusTarefa.PENDENTE));
    }

    // Resolver Divergência (Aprovar/Aceitar a diferença)
    @PostMapping("/{id}/resolver")
    @PreAuthorize("hasAuthority('RECEBIMENTO_FINALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> resolver(
            @PathVariable Long id,
            @RequestParam boolean aceitar,
            @RequestParam String observacao,
            Authentication auth) {

        workflowService.resolverDivergencia(id, aceitar, observacao, auth.getName());
        return ResponseEntity.ok().build();
    }
}