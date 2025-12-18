package br.com.hacerfak.coreWMS.modules.expedicao.controller;

import br.com.hacerfak.coreWMS.modules.expedicao.domain.TarefaSeparacao;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.TarefaSeparacaoRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.service.PickingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/picking")
@RequiredArgsConstructor
public class PickingController {

    private final PickingService pickingService;
    private final TarefaSeparacaoRepository tarefaRepository;

    // 1. O Operador consulta: "O que tenho pra fazer do Pedido X?"
    @GetMapping("/tarefas/{pedidoId}")
    @PreAuthorize("hasAuthority('EXPEDICAO_SEPARAR') or hasRole('ADMIN')")
    public ResponseEntity<List<TarefaSeparacao>> buscarTarefasDoPedido(@PathVariable Long pedidoId) {
        // Retorna apenas o que ainda não foi concluído
        return ResponseEntity.ok(tarefaRepository.findByPedidoIdAndConcluidaFalse(pedidoId));
    }

    // 2. O Operador confirma: "Peguei X unidades e levei para a Doca Y"
    @PostMapping("/tarefas/{tarefaId}/confirmar")
    @PreAuthorize("hasAuthority('EXPEDICAO_SEPARAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> confirmarSeparacao(
            @PathVariable Long tarefaId,
            @RequestParam Long docaId,
            Authentication authentication) { // <--- 1. Injeta

        String usuarioLogado = authentication.getName(); // <--- 2. Pega

        // 3. Passa para o service
        pickingService.confirmarSeparacao(tarefaId, docaId, usuarioLogado);

        return ResponseEntity.ok().build();
    }
}
