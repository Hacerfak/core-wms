package br.com.hacerfak.coreWMS.modules.operacao.controller;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.operacao.domain.SolicitacaoEntrada;
import br.com.hacerfak.coreWMS.modules.operacao.domain.TarefaConferencia;
import br.com.hacerfak.coreWMS.modules.operacao.dto.GerarLpnMassaRequest;
import br.com.hacerfak.coreWMS.modules.operacao.dto.ProgressoRecebimentoDTO;
import br.com.hacerfak.coreWMS.modules.operacao.dto.SolicitacaoEntradaResumoDTO;
import br.com.hacerfak.coreWMS.modules.operacao.repository.SolicitacaoEntradaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.repository.TarefaConferenciaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.service.RecebimentoWorkflowService;
import br.com.hacerfak.coreWMS.modules.operacao.service.NfeImportService;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Lpn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/recebimentos") // Mantive a rota base para facilitar, mas conceitualmente é "/api/inbound"
@RequiredArgsConstructor
public class SolicitacaoEntradaController {

    private final SolicitacaoEntradaRepository solicitacaoRepository;
    private final TarefaConferenciaRepository tarefaRepository;
    private final RecebimentoWorkflowService inboundWorkflowService;
    private final NfeImportService nfeImportService;

    // ========================================================================
    // 1. GESTÃO (Visão do Gerente/Painel)
    // ========================================================================

    @GetMapping
    @PreAuthorize("hasAuthority('RECEBIMENTO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<SolicitacaoEntradaResumoDTO>> listar() {
        return ResponseEntity.ok(solicitacaoRepository.findAllResumo());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RECEBIMENTO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<SolicitacaoEntrada> buscarPorId(@PathVariable Long id) {
        return solicitacaoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================================================================
    // 2. IMPORTAÇÃO (Start do Processo)
    // ========================================================================

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('RECEBIMENTO_IMPORTAR_XML') or hasRole('ADMIN')")
    public ResponseEntity<SolicitacaoEntrada> importarNfe(@RequestParam("file") MultipartFile file) {
        // O Service já devolve a solicitação criada com status e tarefas iniciais
        return ResponseEntity.ok(nfeImportService.importarXml(file));
    }

    // ========================================================================
    // 3. OPERAÇÃO (Visão do Operador/Coletor)
    // ========================================================================

    /**
     * Endpoint para o COLETOR: "Quais tarefas de conferência tenho pendentes?"
     * Útil para o operador selecionar o que vai trabalhar.
     */
    @GetMapping("/tarefas/pendentes")
    @PreAuthorize("hasAuthority('RECEBIMENTO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<List<TarefaConferencia>> listarTarefasPendentes(
            Authentication auth,
            @RequestParam(required = false) Boolean somenteMinhas,
            @RequestParam(required = false) Boolean semAtribuicao) {

        List<StatusTarefa> statusAtivos = List.of(StatusTarefa.PENDENTE, StatusTarefa.EM_EXECUCAO);

        List<TarefaConferencia> tarefas;

        if (Boolean.TRUE.equals(somenteMinhas)) {
            tarefas = tarefaRepository.findByUsuarioAtribuidoAndStatusIn(auth.getName(), statusAtivos);
        } else if (Boolean.TRUE.equals(semAtribuicao)) {
            tarefas = tarefaRepository.findByUsuarioAtribuidoIsNullAndStatusIn(statusAtivos);
        } else {
            tarefas = tarefaRepository.findByStatusIn(statusAtivos);
        }

        return ResponseEntity.ok(tarefas);
    }

    /**
     * Endpoint para o COLETOR: Assumir/Iniciar uma tarefa específica.
     */
    @PostMapping("/tarefas/{tarefaId}/iniciar")
    @PreAuthorize("hasAuthority('RECEBIMENTO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<Void> iniciarTarefa(@PathVariable Long tarefaId, Authentication auth) {
        TarefaConferencia tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));

        tarefa.iniciar(auth.getName());
        tarefaRepository.save(tarefa);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/conferencia-massa")
    @PreAuthorize("hasAuthority('RECEBIMENTO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> conferenciaEmMassa(
            @PathVariable Long id,
            @RequestBody @Valid GerarLpnMassaRequest dto,
            Authentication auth) {

        // Força o ID da URL no DTO para segurança
        if (!id.equals(dto.solicitacaoId())) {
            throw new IllegalArgumentException("ID da URL diverge do corpo da requisição");
        }

        List<String> lpns = inboundWorkflowService.realizarConferenciaEmMassa(dto, auth.getName());

        return ResponseEntity.ok(lpns);
    }

    // --- GESTÃO DE LPNs NA CONFERÊNCIA ---

    @GetMapping("/{id}/lpns")
    @PreAuthorize("hasAuthority('RECEBIMENTO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Lpn>> listarLpnsDaSolicitacao(@PathVariable Long id) {
        return ResponseEntity.ok(inboundWorkflowService.listarLpnsPorSolicitacao(id));
    }

    @DeleteMapping("/{id}/lpns/{lpnId}")
    @PreAuthorize("hasAuthority('RECEBIMENTO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<Void> estornarLpn(
            @PathVariable Long id,
            @PathVariable Long lpnId,
            Authentication auth) {

        inboundWorkflowService.estornarLpn(id, lpnId, auth.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/progresso")
    public ResponseEntity<ProgressoRecebimentoDTO> getProgresso(@PathVariable Long id) {
        return ResponseEntity.ok(inboundWorkflowService.getProgresso(id));
    }

    @GetMapping("/{id}/config-conferencia")
    @PreAuthorize("hasAuthority('RECEBIMENTO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> consultarConfigConferencia(@PathVariable Long id) {
        boolean isCega = inboundWorkflowService.isConferenciaCega(id);
        return ResponseEntity.ok(isCega);
    }

    @PostMapping("/{id}/finalizar")
    @PreAuthorize("hasAuthority('RECEBIMENTO_FINALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> finalizar(@PathVariable Long id, @RequestParam Long stageId, Authentication auth) {
        // Busca a tarefa de conferência pendente/em execução desta solicitação
        // Como simplificação, pegamos a primeira ativa (já que a relação é 1:N mas o
        // fluxo atual é linear)
        TarefaConferencia tarefa = tarefaRepository.findBySolicitacaoPaiIdAndStatus(id, StatusTarefa.EM_EXECUCAO)
                .stream().findFirst()
                .or(() -> tarefaRepository.findBySolicitacaoPaiIdAndStatus(id, StatusTarefa.PENDENTE).stream()
                        .findFirst())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nenhuma tarefa de conferência ativa encontrada para esta solicitação."));

        // Chama o serviço de workflow para concluir
        inboundWorkflowService.concluirConferencia(tarefa.getId(), stageId, auth.getName());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('RECEBIMENTO_CANCELAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, Authentication auth) {
        // CORREÇÃO: Chamada do serviço implementada
        inboundWorkflowService.cancelarRecebimento(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/atribuir-doca")
    @PreAuthorize("hasAuthority('RECEBIMENTO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> atribuirDoca(@PathVariable Long id, @RequestParam Long docaId) {
        inboundWorkflowService.vincularDoca(id, docaId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resetar")
    @PreAuthorize("hasAuthority('RECEBIMENTO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<Void> resetarConferencia(@PathVariable Long id, Authentication auth) {
        inboundWorkflowService.resetarConferencia(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('RECEBIMENTO_EXCLUIR') or hasRole('ADMIN')")
    public ResponseEntity<Void> excluirSolicitacao(@PathVariable Long id, Authentication auth) {
        inboundWorkflowService.excluirSolicitacao(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}