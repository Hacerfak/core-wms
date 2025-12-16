package br.com.hacerfak.coreWMS.modules.expedicao.controller;

import br.com.hacerfak.coreWMS.modules.expedicao.domain.PedidoSaida;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.StatusPedido;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.PedidoSaidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/expedicao")
@RequiredArgsConstructor
public class ExpedicaoController {

    private final PedidoSaidaRepository pedidoRepository;

    // Simula o caminhão saindo (Sem NFe por enquanto)
    @PostMapping("/despachar/{pedidoId}")
    public ResponseEntity<Void> despacharPedido(@PathVariable Long pedidoId) {
        PedidoSaida pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setStatus(StatusPedido.DESPACHADO);
        pedidoRepository.save(pedido);

        return ResponseEntity.ok().build();
    }
}