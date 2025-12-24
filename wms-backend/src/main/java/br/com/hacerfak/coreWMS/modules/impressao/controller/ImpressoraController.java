package br.com.hacerfak.coreWMS.modules.impressao.controller;

import br.com.hacerfak.coreWMS.modules.impressao.domain.Impressora;
import br.com.hacerfak.coreWMS.modules.impressao.dto.ImpressoraRequest;
import br.com.hacerfak.coreWMS.modules.impressao.repository.ImpressoraRepository;
import br.com.hacerfak.coreWMS.modules.impressao.service.ImpressaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/impressao/impressoras")
@RequiredArgsConstructor
public class ImpressoraController {

    private final ImpressoraRepository impressoraRepository;
    private final ImpressaoService impressaoService;

    @GetMapping
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<List<Impressora>> listar() {
        return ResponseEntity.ok(impressoraRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONFIG_SISTEMA') or hasRole('ADMIN')")
    public ResponseEntity<Impressora> criar(@RequestBody @Valid ImpressoraRequest dto) {
        return ResponseEntity.ok(impressaoService.cadastrarImpressora(dto));
    }
}