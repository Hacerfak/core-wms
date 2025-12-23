package br.com.hacerfak.coreWMS.modules.expedicao.controller;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.OndaSeparacao;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.SolicitacaoSaida;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.TarefaSeparacao;
import br.com.hacerfak.coreWMS.modules.expedicao.dto.SolicitacaoSaidaRequest;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.OndaSeparacaoRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.TarefaSeparacaoRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.service.OutboundWorkflowService;
import br.com.hacerfak.coreWMS.modules.expedicao.service.PickingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expedicao")
@RequiredArgsConstructor
public class ExpedicaoController {

    private final OutboundWorkflowService workflowService;
    private final PickingService pickingService;
    private final TarefaSeparacaoRepository tarefaRepository;
    private final OndaSeparacaoRepository ondaRepository;

    // --- GESTÃO (ERP/Painel) ---

    @PostMapping("/solicitacoes")
    @PreAuthorize("hasAuthority('PEDIDO_CRIAR') or hasRole('ADMIN')")
    public ResponseEntity<SolicitacaoSaida> criarSolicitacao(@RequestBody @Valid SolicitacaoSaidaRequest dto) {
        return ResponseEntity.ok(workflowService.criarSolicitacao(dto));
    }

    @PostMapping("/ondas/gerar-automatica")
    @PreAuthorize("hasAuthority('PEDIDO_ALOCAR') or hasRole('ADMIN')")
    public ResponseEntity<OndaSeparacao> gerarOnda() {
        // Agrupa tudo o que está pendente em uma nova onda
        return ResponseEntity.ok(workflowService.gerarOndaAutomatica());
    }

    @PostMapping("/ondas/{id}/processar")
    @PreAuthorize("hasAuthority('PEDIDO_ALOCAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> processarOnda(@PathVariable Long id) {
        // Roda o FEFO e gera tarefas
        workflowService.processarOnda(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ondas")
    @PreAuthorize("hasAuthority('PEDIDO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<OndaSeparacao>> listarOndas() {
        return ResponseEntity.ok(ondaRepository.findAll());
    }

    // --- COLETOR (Picking) ---

    @GetMapping("/tarefas/pendentes")
    @PreAuthorize("hasAuthority('EXPEDICAO_SEPARAR') or hasRole('ADMIN')")
    public ResponseEntity<List<TarefaSeparacao>> listarPickingPendente() {
        return ResponseEntity.ok(tarefaRepository.findByStatus(StatusTarefa.PENDENTE));
    }

    @PostMapping("/tarefas/{id}/confirmar")
    @PreAuthorize("hasAuthority('EXPEDICAO_SEPARAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> confirmarPicking(
            @PathVariable Long id,
            @RequestParam Long localDestinoId, // Doca ou Stage
            Authentication auth) {

        pickingService.confirmarSeparacao(id, localDestinoId, auth.getName());
        return ResponseEntity.ok().build();
    }
}