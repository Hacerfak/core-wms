package br.com.hacerfak.coreWMS.modules.expedicao.service;

import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.*;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlocacaoService {

    private final PedidoSaidaRepository pedidoRepository;
    private final EstoqueSaldoRepository saldoRepository;
    private final TarefaSeparacaoRepository tarefaRepository;

    @Transactional
    public void alocarPedido(Long pedidoId) {
        PedidoSaida pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        if (pedido.getStatus() != StatusPedido.CRIADO) {
            throw new RuntimeException("Pedido já foi processado.");
        }

        for (ItemPedido item : pedido.getItens()) {
            BigDecimal qtdFaltante = item.getQuantidadeSolicitada();

            // 1. Busca saldos disponíveis (ordenados por validade ASC = FEFO)
            // Precisamos criar esse método no Repository que considera (Qtd - Reservado) >
            // 0
            List<EstoqueSaldo> saldos = saldoRepository.buscarDisponiveisPorValidade(item.getProduto().getId());

            for (EstoqueSaldo saldo : saldos) {
                if (qtdFaltante.compareTo(BigDecimal.ZERO) <= 0)
                    break;

                BigDecimal disponivelNoLote = saldo.getQuantidade().subtract(saldo.getQuantidadeReservada());

                if (disponivelNoLote.compareTo(BigDecimal.ZERO) <= 0)
                    continue;

                // Define quanto vamos pegar deste lote específico
                BigDecimal aReservar = disponivelNoLote.min(qtdFaltante);

                // 2. Cria a Tarefa para o Operador
                TarefaSeparacao tarefa = TarefaSeparacao.builder()
                        .pedido(pedido)
                        .produto(item.getProduto())
                        .localizacaoOrigem(saldo.getLocalizacao())
                        .loteAlocado(saldo.getLote())
                        .quantidadePlanejada(aReservar)
                        .concluida(false)
                        .build();
                tarefaRepository.save(tarefa);

                // 3. Bloqueia o saldo (Atualiza tabela de estoque)
                saldo.setQuantidadeReservada(saldo.getQuantidadeReservada().add(aReservar));
                saldoRepository.save(saldo);

                // Atualiza contadores
                item.setQuantidadeAlocada(item.getQuantidadeAlocada().add(aReservar)); // Bug fix: era
                                                                                       // setQuantidadeAlocada
                qtdFaltante = qtdFaltante.subtract(aReservar);
            }

            // Aqui você pode verificar se qtdFaltante > 0 e marcar o pedido como "CORTE"
            // (falta de estoque)
        }

        pedido.setStatus(StatusPedido.ALOCADO);
        pedidoRepository.save(pedido);
    }
}
