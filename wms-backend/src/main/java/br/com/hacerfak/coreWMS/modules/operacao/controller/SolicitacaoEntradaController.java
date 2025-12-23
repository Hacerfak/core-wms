package br.com.hacerfak.coreWMS.modules.operacao.controller;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.operacao.domain.SolicitacaoEntrada;
import br.com.hacerfak.coreWMS.modules.operacao.domain.TarefaConferencia;
import br.com.hacerfak.coreWMS.modules.operacao.dto.GerarLpnMassaRequest;
import br.com.hacerfak.coreWMS.modules.operacao.dto.SolicitacaoEntradaResumoDTO;
import br.com.hacerfak.coreWMS.modules.operacao.repository.SolicitacaoEntradaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.repository.TarefaConferenciaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.service.RecebimentoWorkflowService;
import br.com.hacerfak.coreWMS.modules.operacao.service.NfeImportService;
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
        // Define o status alvo
        StatusTarefa statusAlvo = StatusTarefa.PENDENTE;

        if (Boolean.TRUE.equals(somenteMinhas)) {
            // CORREÇÃO: Usando a variável 'statusAlvo' ao invés do Enum direto
            return ResponseEntity.ok(tarefaRepository.findByUsuarioAtribuidoAndStatus(auth.getName(), statusAlvo));
        }

        if (Boolean.TRUE.equals(semAtribuicao)) {
            // CORREÇÃO: Usando a variável 'statusAlvo'
            return ResponseEntity.ok(tarefaRepository.findByUsuarioAtribuidoIsNullAndStatus(statusAlvo));
        }

        // Padrão: Todas as pendentes
        // CORREÇÃO: Usando a variável 'statusAlvo'
        return ResponseEntity.ok(tarefaRepository.findByStatus(statusAlvo));
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

    // OBS: Os endpoints de "Bipar Produto" (gerarVolume) e "Finalizar"
    // serão reimplementados na próxima etapa, focados na TarefaConferencia
    // e não mais no ID da Nota Fiscal.
}