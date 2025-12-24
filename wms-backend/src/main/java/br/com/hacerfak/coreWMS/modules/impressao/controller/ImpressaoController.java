package br.com.hacerfak.coreWMS.modules.impressao.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.impressao.domain.EtiquetaTemplate;
import br.com.hacerfak.coreWMS.modules.impressao.repository.EtiquetaTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/impressao/templates")
@RequiredArgsConstructor
public class ImpressaoController {

    private final EtiquetaTemplateRepository templateRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<List<EtiquetaTemplate>> listar() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<EtiquetaTemplate> criar(@RequestBody EtiquetaTemplate template) {
        if (template.isPadrao()) {
            desmarcarOutrosPadroes(template);
        }
        return ResponseEntity.ok(templateRepository.save(template));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<EtiquetaTemplate> atualizar(@PathVariable Long id, @RequestBody EtiquetaTemplate dto) {
        EtiquetaTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template não encontrado"));

        template.setNome(dto.getNome());
        template.setTipoFinalidade(dto.getTipoFinalidade());
        template.setZplCodigo(dto.getZplCodigo());
        template.setLarguraMm(dto.getLarguraMm());
        template.setAlturaMm(dto.getAlturaMm());

        // Se mudou para padrão, garante unicidade
        if (!template.isPadrao() && dto.isPadrao()) {
            template.setPadrao(true);
            desmarcarOutrosPadroes(template);
        } else {
            template.setPadrao(dto.isPadrao());
        }

        return ResponseEntity.ok(templateRepository.save(template));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (!templateRepository.existsById(id)) {
            throw new EntityNotFoundException("Template não encontrado");
        }
        templateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Auxiliar para manter apenas 1 padrão por tipo
    private void desmarcarOutrosPadroes(EtiquetaTemplate novoOuEditado) {
        templateRepository.findFirstByTipoFinalidadeAndPadraoTrue(novoOuEditado.getTipoFinalidade())
                .ifPresent(antigoPadrao -> {
                    if (!antigoPadrao.getId().equals(novoOuEditado.getId())) {
                        antigoPadrao.setPadrao(false);
                        templateRepository.save(antigoPadrao);
                    }
                });
    }
}