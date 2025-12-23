package br.com.hacerfak.coreWMS.modules.faturamento.controller;

import br.com.hacerfak.coreWMS.modules.faturamento.domain.ApontamentoServico;
import br.com.hacerfak.coreWMS.modules.faturamento.dto.ExtratoCobrancaDTO;
import br.com.hacerfak.coreWMS.modules.faturamento.service.FaturamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/faturamento")
@RequiredArgsConstructor
public class FaturamentoController {

    private final FaturamentoService faturamentoService;

    /**
     * Endpoint para lançar serviços manuais.
     */
    @PostMapping("/apontamento-manual")
    @PreAuthorize("hasAuthority('FATURAMENTO_APONTAR') or hasRole('ADMIN')")
    public ResponseEntity<ApontamentoServico> apontarServico(
            @RequestParam Long clienteId,
            @RequestParam String codigoServico,
            @RequestParam BigDecimal quantidade,
            @RequestParam(required = false) String observacao,
            Authentication authentication) {

        ApontamentoServico apontamento = faturamentoService.apontamentoManual(
                clienteId,
                codigoServico,
                quantidade,
                observacao,
                authentication.getName());

        return ResponseEntity.ok(apontamento);
    }

    /**
     * Relatório/Extrato de Cobrança por período.
     */
    @GetMapping("/extrato")
    @PreAuthorize("hasAuthority('FATURAMENTO_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<ExtratoCobrancaDTO> gerarExtrato(
            @RequestParam Long clienteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        ExtratoCobrancaDTO relatorio = faturamentoService.gerarExtratoPeriodo(clienteId, inicio, fim);
        return ResponseEntity.ok(relatorio);
    }
}