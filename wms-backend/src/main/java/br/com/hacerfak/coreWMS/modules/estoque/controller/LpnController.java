package br.com.hacerfak.coreWMS.modules.estoque.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Lpn;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.ImpressaoService;
import br.com.hacerfak.coreWMS.modules.impressao.service.ZplGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque/lpns")
@RequiredArgsConstructor
public class LpnController {

    private final LpnRepository lpnRepository;
    private final ZplGeneratorService zplService;
    private final ImpressaoService impressaoService;

    // Listagem simples (útil para testes ou consultas)
    @GetMapping
    @PreAuthorize("hasAuthority('ESTOQUE_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<List<Lpn>> listarLpns() {
        return ResponseEntity.ok(lpnRepository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ESTOQUE_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Lpn> buscarPorId(@PathVariable Long id) {
        return lpnRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EntityNotFoundException("LPN não encontrada"));
    }

    /**
     * PREVIEW: Apenas retorna o código ZPL para validação visual (não imprime).
     */
    @GetMapping(value = "/{id}/etiqueta/preview", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAuthority('ESTOQUE_MOVIMENTAR') or hasRole('ADMIN')")
    public ResponseEntity<String> visualizarEtiqueta(
            @PathVariable Long id,
            @RequestParam(required = false) Long templateId) {

        Lpn lpn = lpnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("LPN não encontrada"));

        String zpl = zplService.gerarZplParaLpn(templateId, lpn);
        return ResponseEntity.ok(zpl);
    }

    /**
     * IMPRESSÃO REAL: Gera o ZPL e envia para a fila do Agente.
     */
    @PostMapping("/{id}/imprimir")
    @PreAuthorize("hasAuthority('ESTOQUE_MOVIMENTAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> enviarParaImpressora(
            @PathVariable Long id,
            @RequestParam(required = false) Long templateId,
            @RequestParam Long impressoraId) { // O Front deve enviar o ID da impressora selecionada

        Lpn lpn = lpnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("LPN não encontrada"));

        // 1. Gera o conteúdo ZPL
        String zpl = zplService.gerarZplParaLpn(templateId, lpn);

        // 2. Pega o usuário logado para auditoria
        String usuario = "SISTEMA";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            usuario = auth.getName();
        }

        // 3. Envia para a fila (O Agente vai ler daqui)
        // OBS: Certifique-se que seu ImpressaoService tem o método enviarParaFila
        // aceitando ID da impressora
        impressaoService.enviarParaFila(zpl, impressoraId, usuario, "LPN_" + lpn.getCodigo());

        return ResponseEntity.ok().build();
    }
}