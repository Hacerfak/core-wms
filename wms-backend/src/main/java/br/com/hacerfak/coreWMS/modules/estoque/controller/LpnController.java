package br.com.hacerfak.coreWMS.modules.estoque.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Lpn;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.ZplGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque/lpns")
@RequiredArgsConstructor
public class LpnController {

    private final LpnRepository lpnRepository;
    private final ZplGeneratorService zplService;

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
     * Endpoint de Impressão: Retorna o ZPL gerado.
     */
    @GetMapping(value = "/{id}/etiqueta", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAuthority('ESTOQUE_MOVIMENTAR') or hasRole('ADMIN')")
    public ResponseEntity<String> gerarEtiqueta(
            @PathVariable Long id,
            @RequestParam(required = false) Long templateId) {

        Lpn lpn = lpnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("LPN não encontrada"));

        String zpl = zplService.gerarZplParaLpn(templateId, lpn);
        return ResponseEntity.ok(zpl);
    }
}