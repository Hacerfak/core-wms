package br.com.hacerfak.coreWMS.modules.impressao.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.impressao.domain.FilaImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.dto.PrintJobDTO;
import br.com.hacerfak.coreWMS.modules.impressao.repository.FilaImpressaoRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.ImpressaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Pode exigir permissão técnica
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/impressao/fila")
@RequiredArgsConstructor
public class FilaImpressaoController {

    private final ImpressaoService impressaoService;
    private final FilaImpressaoRepository filaRepository;

    // --- MÉTODOS PARA O AGENTE ---

    /**
     * O Agente chama isso a cada N segundos para pegar trabalho.
     * Idealmente, protegido por uma API Key ou Token de Serviço.
     */
    @GetMapping("/pendentes")
    public ResponseEntity<List<PrintJobDTO>> buscarPendentes() {
        return ResponseEntity.ok(impressaoService.buscarTrabalhosPendentes());
    }

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