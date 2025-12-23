package br.com.hacerfak.coreWMS.modules.expedicao.controller;

import br.com.hacerfak.coreWMS.modules.expedicao.domain.VolumeExpedicao;
import br.com.hacerfak.coreWMS.modules.expedicao.service.PackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/expedicao/packing")
@RequiredArgsConstructor
public class PackingController {

    private final PackingService packingService;

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
}