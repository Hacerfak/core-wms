package br.com.hacerfak.coreWMS.modules.portaria.controller;

import br.com.hacerfak.coreWMS.modules.portaria.service.PortariaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/operacao-patio")
@RequiredArgsConstructor
public class PortariaOperacaoController {

    private final PortariaService portariaService;

    @PostMapping("/entrada/{solicitacaoId}/encostar")
    @PreAuthorize("hasAuthority('RECEBIMENTO_OPERAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> encostarEntrada(
            @PathVariable Long solicitacaoId,
            @RequestParam Long docaId) {

        portariaService.encostarVeiculoPelaSolicitacao(solicitacaoId, docaId, "ENTRADA");
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/entrada/{solicitacaoId}/liberar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('RECEBIMENTO_FINALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> liberarEntrada(
            @PathVariable Long solicitacaoId,
            @RequestParam("assinatura") MultipartFile assinatura) {

        portariaService.liberarSaidaComAssinatura(solicitacaoId, assinatura, "ENTRADA");
        return ResponseEntity.ok().build();
    }
}