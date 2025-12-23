package br.com.hacerfak.coreWMS.modules.estoque.controller;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TarefaArmazenagem;
import br.com.hacerfak.coreWMS.modules.estoque.dto.ArmazenagemRequest;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.TarefaArmazenagemRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.ArmazenagemWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final ArmazenagemWorkflowService armazenagemService;
    private final EstoqueSaldoRepository saldoRepository;
    private final TarefaArmazenagemRepository tarefaRepository;

    // --- COLETOR: LISTA DE TAREFAS (PUT-AWAY) ---
    @GetMapping("/tarefas/pendentes")
    @PreAuthorize("hasAuthority('ESTOQUE_ARMAZENAR') or hasRole('ADMIN')")
    public ResponseEntity<List<TarefaArmazenagem>> listarTarefasPendentes() {
        // Retorna tudo que precisa ser guardado
        return ResponseEntity.ok(tarefaRepository.findByStatus(StatusTarefa.PENDENTE));
    }

    // --- COLETOR: CONFIRMAR ARMAZENAGEM ---
    @PostMapping("/tarefas/{tarefaId}/confirmar")
    @PreAuthorize("hasAuthority('ESTOQUE_ARMAZENAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> confirmarArmazenagem(
            @PathVariable Long tarefaId,
            @RequestBody @Valid ArmazenagemRequest dto, // Reaproveitando o DTO que tem localDestinoId
            Authentication authentication) {

        armazenagemService.confirmarArmazenagem(
                tarefaId,
                dto.localDestinoId(),
                authentication.getName());

        return ResponseEntity.ok().build();
    }

    // --- CONSULTAS (DASHBOARD) ---
    @GetMapping("/detalhado")
    @PreAuthorize("hasAuthority('ESTOQUE_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<EstoqueSaldo>> saldoDetalhado() {
        return ResponseEntity.ok(saldoRepository.findAllCompleto());
    }
}