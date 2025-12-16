package br.com.hacerfak.coreWMS.modules.estoque.controller;

import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import br.com.hacerfak.coreWMS.modules.estoque.dto.ArmazenagemRequest;
import br.com.hacerfak.coreWMS.modules.estoque.dto.MovimentacaoRequest;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.EstoqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;
    private final EstoqueSaldoRepository saldoRepository;

    // --- 1. OPERAÇÃO: ARMAZENAGEM DE LPN (Putaway) ---
    @PostMapping("/armazenar")
    public ResponseEntity<Void> armazenarLpn(
            @RequestBody @Valid ArmazenagemRequest dto,
            Authentication authentication) { // <--- O Spring injeta o usuário logado aqui

        // Pega o nome do usuário direto do Token JWT
        String usuarioLogado = authentication.getName();

        estoqueService.armazenarLpn(dto.lpn(), dto.localDestinoId(), usuarioLogado);

        return ResponseEntity.ok().build();
    }

    // --- 2. OPERAÇÃO: AJUSTES E MOVIMENTAÇÕES MANUAIS ---
    @PostMapping("/movimentar")
    public ResponseEntity<Void> movimentar(
            @RequestBody @Valid MovimentacaoRequest dto,
            Authentication authentication) { // <--- Injeção de Segurança

        String usuarioLogado = authentication.getName();

        estoqueService.movimentar(
                dto.produtoId(),
                dto.localizacaoId(),
                dto.quantidade(),
                null, // LPN (Se quiser mover LPN manualmente, precisamos atualizar o DTO depois)
                dto.lote(),
                dto.numeroSerie(),
                dto.tipo(),
                usuarioLogado, // <--- Auditoria Real
                dto.observacao());

        return ResponseEntity.ok().build();
    }

    // --- 3. CONSULTA: SALDOS ---

    // Visão Geral (Soma simples por produto)
    @GetMapping("/produto/{produtoId}/total")
    public ResponseEntity<Double> saldoTotal(@PathVariable Long produtoId) {
        return ResponseEntity.ok(saldoRepository.somarEstoqueDoProduto(produtoId));
    }

    // Visão Detalhada (Lotes, LPNs, Locais)
    @GetMapping("/detalhado")
    public ResponseEntity<List<EstoqueSaldo>> saldoDetalhado() {
        return ResponseEntity.ok(saldoRepository.findAll());
    }
}