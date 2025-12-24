package br.com.hacerfak.coreWMS.modules.expedicao.controller;

import br.com.hacerfak.coreWMS.modules.expedicao.domain.OndaSeparacao;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.SolicitacaoSaida;
import br.com.hacerfak.coreWMS.modules.expedicao.dto.SolicitacaoSaidaRequest;
import br.com.hacerfak.coreWMS.modules.expedicao.service.OutboundWorkflowService;
import br.com.hacerfak.coreWMS.modules.expedicao.service.PickingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/expedicao")
@RequiredArgsConstructor
public class ExpedicaoController {

    private final OutboundWorkflowService outboundService;
    private final PickingService pickingService;

    // 1. Criação de Pedidos (Integração ou Manual)
    @PostMapping("/pedidos")
    @PreAuthorize("hasAuthority('EXPEDICAO_CRIAR') or hasRole('ADMIN')")
    public ResponseEntity<SolicitacaoSaida> criarSolicitacao(@RequestBody @Valid SolicitacaoSaidaRequest request) {
        // CORREÇÃO: Método correto é 'criarSolicitacao'
        return ResponseEntity.ok(outboundService.criarSolicitacao(request));
    }

    // 2. Geração de Ondas (Planejamento)
    @PostMapping("/ondas/gerar")
    @PreAuthorize("hasAuthority('EXPEDICAO_PLANEJAR') or hasRole('ADMIN')")
    public ResponseEntity<OndaSeparacao> gerarOndas(
            @RequestParam(required = false) String rota) { // <--- Parâmetro adicionado

        // Agora passamos a rota (ou null) para o serviço
        return ResponseEntity.ok(outboundService.gerarOndaAutomatica(rota));
    }

    // 3. Processamento de Onda (Alocação - Passo manual opcional se não for
    // automático)
    @PostMapping("/ondas/{id}/processar")
    @PreAuthorize("hasAuthority('PEDIDO_ALOCAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> processarOnda(@PathVariable Long id) {
        outboundService.processarOnda(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 4. Confirmação de Tarefa (Picking)
     * Método atualizado para suportar Short Pick (Corte)
     */
    @PostMapping("/tarefas/{tarefaId}/confirmar")
    @PreAuthorize("hasAuthority('EXPEDICAO_SEPARAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> confirmarSeparacao(
            @PathVariable Long tarefaId,
            @RequestParam Long docaId,
            @RequestParam(required = false) BigDecimal quantidadeConfirmada,
            Authentication authentication) {

        pickingService.confirmarSeparacao(tarefaId, docaId, quantidadeConfirmada, authentication.getName());

        return ResponseEntity.ok().build();
    }

    // --- MELHORIA 3: CHECK-OUT (Carregamento) ---
    /**
     * Endpoint para bipar o volume na doca e confirmar o despacho.
     * Ex: POST /api/expedicao/despachar/VOL-123456
     */
    @PostMapping("/despachar/{codigoRastreio}")
    @PreAuthorize("hasAuthority('EXPEDICAO_DESPACHAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> confirmarDespachoVolume(
            @PathVariable String codigoRastreio,
            Authentication auth) {

        outboundService.realizarConferenciaExpedicao(codigoRastreio, auth.getName());
        return ResponseEntity.ok().build();
    }
}