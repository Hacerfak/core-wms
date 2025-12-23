package br.com.hacerfak.coreWMS.modules.estoque.controller;

import br.com.hacerfak.coreWMS.modules.estoque.dto.relatorio.*;
import br.com.hacerfak.coreWMS.modules.estoque.service.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/posicao-estoque")
    @PreAuthorize("hasAuthority('RELATORIO_ESTOQUE') or hasRole('ADMIN')")
    public ResponseEntity<List<PosicaoEstoqueDTO>> posicaoEstoque() {
        return ResponseEntity.ok(relatorioService.gerarRelatorioPosicaoEstoque());
    }

    @GetMapping("/kardex/{produtoId}")
    @PreAuthorize("hasAuthority('RELATORIO_ESTOQUE') or hasRole('ADMIN')")
    public ResponseEntity<List<KardexDTO>> kardexProduto(@PathVariable Long produtoId) {
        return ResponseEntity.ok(relatorioService.gerarKardexProduto(produtoId));
    }

    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('RELATORIO_ESTOQUE') or hasRole('ADMIN')")
    public ResponseEntity<List<AgingDTO>> aging(@RequestParam(required = false) Integer diasVencimento) {
        return ResponseEntity.ok(relatorioService.gerarRelatorioAging(diasVencimento));
    }

    @GetMapping("/acuracidade")
    @PreAuthorize("hasAuthority('RELATORIO_ESTOQUE') or hasRole('ADMIN')")
    public ResponseEntity<List<AcuracidadeDTO>> acuracidade() {
        return ResponseEntity.ok(relatorioService.gerarRelatorioAcuracidade());
    }

    @GetMapping("/ocupacao")
    @PreAuthorize("hasAuthority('RELATORIO_ESTOQUE') or hasRole('ADMIN')")
    public ResponseEntity<List<OcupacaoDTO>> ocupacao() {
        return ResponseEntity.ok(relatorioService.gerarRelatorioOcupacao());
    }
}