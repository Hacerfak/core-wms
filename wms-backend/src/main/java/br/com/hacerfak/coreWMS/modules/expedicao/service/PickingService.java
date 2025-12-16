package br.com.hacerfak.coreWMS.modules.expedicao.service;

import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoMovimento;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.EstoqueService;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.TarefaSeparacao;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.TarefaSeparacaoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PickingService {

        private final TarefaSeparacaoRepository tarefaRepository;
        private final EstoqueService estoqueService;
        private final EstoqueSaldoRepository saldoRepository;

        @Transactional
        public void confirmarSeparacao(Long tarefaId, Long localDestinoId, String usuarioResponsavel) {
                TarefaSeparacao tarefa = tarefaRepository.findById(tarefaId)
                                .orElseThrow(() -> new RuntimeException("Tarefa não encontrada"));

                if (tarefa.isConcluida())
                        return;

                // 1. Baixa a Reserva do Saldo Original
                // CORREÇÃO AQUI: Adicionado 'null' para o parâmetro LPN que faltava
                EstoqueSaldo saldoOrigem = saldoRepository.buscarSaldoExato(
                                tarefa.getProduto().getId(),
                                tarefa.getLocalizacaoOrigem().getId(),
                                null, // <--- LPN (Como a tarefa não tem LPN gravado, passamos null)
                                tarefa.getLoteAlocado(),
                                null // Serial
                ).orElseThrow(() -> new RuntimeException(
                                "Saldo sumiu ou não foi encontrado (Verifique se o estoque possui LPN)"));

                saldoOrigem
                                .setQuantidadeReservada(saldoOrigem.getQuantidadeReservada()
                                                .subtract(tarefa.getQuantidadePlanejada()));
                saldoRepository.save(saldoOrigem);

                // 2. Movimenta de VERDADE
                estoqueService.movimentar(
                                tarefa.getProduto().getId(),
                                tarefa.getLocalizacaoOrigem().getId(),
                                tarefa.getQuantidadePlanejada(),
                                null,
                                tarefa.getLoteAlocado(),
                                null,
                                TipoMovimento.SAIDA,
                                usuarioResponsavel, // <--- AUDITORIA REAL
                                "Picking Pedido " + tarefa.getPedido().getCodigoPedidoExterno());

                estoqueService.movimentar(
                                tarefa.getProduto().getId(),
                                localDestinoId,
                                tarefa.getQuantidadePlanejada(),
                                null,
                                tarefa.getLoteAlocado(),
                                null,
                                TipoMovimento.ENTRADA,
                                usuarioResponsavel, // <--- AUDITORIA REAL
                                "Stage Pedido " + tarefa.getPedido().getCodigoPedidoExterno());

                tarefa.setConcluida(true);
                tarefaRepository.save(tarefa);
        }
}