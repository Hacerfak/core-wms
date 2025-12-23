package br.com.hacerfak.coreWMS.modules.expedicao.controller;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.TarefaSeparacao;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.TarefaSeparacaoRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.service.PickingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/picking")
@RequiredArgsConstructor
public class PickingController {

    private final PickingService pickingService;
    private final TarefaSeparacaoRepository tarefaRepository;

    /**
     * O Operador consulta: "Quais tarefas tenho nesta Onda?"
     * Alterado de 'pedidoId' para 'ondaId' e ajustado para o novo StatusTarefa.
     */
    @GetMapping("/tarefas/onda/{ondaId}")
    @PreAuthorize("hasAuthority('EXPEDICAO_SEPARAR') or hasRole('ADMIN')")
    public ResponseEntity<List<TarefaSeparacao>> buscarTarefasDaOnda(@PathVariable Long ondaId) {
        // Busca tarefas da onda que estão PENDENTES ou EM_EXECUCAO
        // Para simplificar, vamos trazer as PENDENTES
        return ResponseEntity.ok(tarefaRepository.findByOndaIdAndStatus(ondaId, StatusTarefa.PENDENTE));
    }

    /**
     * O Operador consulta: "Me dê todas as tarefas de picking pendentes do armazém"
     * (Modo global, sem filtrar por onda específica)
     */
    @GetMapping("/tarefas/pendentes")
    @PreAuthorize("hasAuthority('EXPEDICAO_SEPARAR') or hasRole('ADMIN')")
    public ResponseEntity<List<TarefaSeparacao>> buscarTodasPendentes() {
        return ResponseEntity.ok(tarefaRepository.findByStatus(StatusTarefa.PENDENTE));
    }

    /**
     * Confirmação de Picking com suporte a Divergência (Corte).
     * Se quantidadeConfirmada < QuantidadePlanejada, gera corte e auditoria.
     */
    @PostMapping("/tarefas/{tarefaId}/confirmar")
    @PreAuthorize("hasAuthority('EXPEDICAO_SEPARAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> confirmarSeparacao(
            @PathVariable Long tarefaId,
            @RequestParam Long docaId,
            @RequestParam(required = false) BigDecimal quantidadeConfirmada, // Opcional, se nulo assume total
            Authentication authentication) {

        pickingService.confirmarSeparacao(tarefaId, docaId, quantidadeConfirmada, authentication.getName());

        return ResponseEntity.ok().build();
    }
}