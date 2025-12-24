package br.com.hacerfak.coreWMS.modules.expedicao.controller;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.VolumeExpedicao;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.VolumeExpedicaoRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.service.PackingService;
import br.com.hacerfak.coreWMS.modules.impressao.service.ZplGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/expedicao/packing")
@RequiredArgsConstructor
public class PackingController {

    private final PackingService packingService;
    private final VolumeExpedicaoRepository volumeRepository;
    private final ZplGeneratorService zplService;

    @PostMapping("/abrir-volume")
    @PreAuthorize("hasAuthority('EXPEDICAO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<VolumeExpedicao> abrirVolume(
            @RequestParam Long solicitacaoId,
            @RequestParam String tipoEmbalagem) { // CAIXA, PALLET
        return ResponseEntity.ok(packingService.abrirNovoVolume(solicitacaoId, tipoEmbalagem));
    }

    @PostMapping("/{volumeId}/bipar")
    @PreAuthorize("hasAuthority('EXPEDICAO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<Void> biparItemOuLpn(
            @PathVariable Long volumeId,
            @RequestParam String codigo) { // Pode ser EAN13 ou LPN

        packingService.conferirItemOuLpn(volumeId, codigo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{volumeId}/fechar")
    @PreAuthorize("hasAuthority('EXPEDICAO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<VolumeExpedicao> fecharVolume(
            @PathVariable Long volumeId,
            @RequestParam(required = false) BigDecimal peso) {
        return ResponseEntity.ok(packingService.fecharVolume(volumeId, peso));
    }

    // --- NOVO: Endpoint de Impressão de Etiqueta de Volume ---
    @GetMapping(value = "/{volumeId}/etiqueta", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAuthority('EXPEDICAO_CONFERIR') or hasRole('ADMIN')")
    public ResponseEntity<String> gerarEtiquetaVolume(
            @PathVariable Long volumeId,
            @RequestParam(required = false) Long templateId) {

        VolumeExpedicao volume = volumeRepository.findById(volumeId)
                .orElseThrow(() -> new EntityNotFoundException("Volume não encontrado"));

        String zpl = zplService.gerarZplParaVolume(templateId, volume);
        return ResponseEntity.ok(zpl);
    }
}