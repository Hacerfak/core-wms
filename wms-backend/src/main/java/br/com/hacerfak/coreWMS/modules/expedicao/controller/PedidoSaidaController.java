package br.com.hacerfak.coreWMS.modules.expedicao.controller;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.ItemPedido;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.PedidoSaida;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.StatusPedido;
import br.com.hacerfak.coreWMS.modules.expedicao.dto.PedidoRequest;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.PedidoSaidaRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.service.AlocacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoSaidaController {

    private final PedidoSaidaRepository pedidoRepository;
    private final ParceiroRepository parceiroRepository;
    private final ProdutoRepository produtoRepository;
    private final AlocacaoService alocacaoService;

    // 1. Criar Pedido (Integração ERP)
    @PostMapping
    @Transactional
    public ResponseEntity<PedidoSaida> criarPedido(@RequestBody PedidoRequest dto) {
        Parceiro cliente = parceiroRepository.findById(dto.clienteId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        PedidoSaida pedido = PedidoSaida.builder()
                .codigoPedidoExterno(dto.codigoExterno())
                .cliente(cliente)
                .status(StatusPedido.CRIADO)
                .itens(new ArrayList<>())
                .build();

        for (PedidoRequest.ItemPedidoRequest itemDto : dto.itens()) {
            Produto produto = produtoRepository.findById(itemDto.produtoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemDto.produtoId()));

            ItemPedido item = ItemPedido.builder()
                    .pedido(pedido)
                    .produto(produto)
                    .quantidadeSolicitada(itemDto.quantidade())
                    .quantidadeAlocada(java.math.BigDecimal.ZERO)
                    .quantidadeSeparada(java.math.BigDecimal.ZERO)
                    .build();

            pedido.getItens().add(item);
        }

        return ResponseEntity.ok(pedidoRepository.save(pedido));
    }

    // 2. Disparar Alocação (Processamento de Reserva)
    // Isso roda o algoritmo FEFO e gera tarefas para o coletor
    @PostMapping("/{id}/alocar")
    public ResponseEntity<String> alocarPedido(@PathVariable Long id) {
        try {
            alocacaoService.alocarPedido(id);
            return ResponseEntity.ok("Pedido alocado com sucesso! Tarefas geradas.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 3. Listar Pedidos
    @GetMapping
    public ResponseEntity<List<PedidoSaida>> listar() {
        return ResponseEntity.ok(pedidoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoSaida> buscarPorId(@PathVariable Long id) {
        return pedidoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelarPedido(@PathVariable Long id) {
        return pedidoRepository.findById(id).map(pedido -> {
            if ("DESPACHADO".equals(pedido.getStatus().toString())) {
                return ResponseEntity.badRequest().body("Pedido já despachado não pode ser excluído.");
            }
            // Se estiver ALOCADO, precisaríamos "Desalocar" (devolver reserva) antes de
            // deletar.
            // Para simplificar agora:
            if ("ALOCADO".equals(pedido.getStatus().toString())) {
                return ResponseEntity.badRequest().body("Cancele a alocação antes de excluir (Feature futura).");
            }

            pedidoRepository.delete(pedido);
            return ResponseEntity.noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
