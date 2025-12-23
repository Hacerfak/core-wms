package br.com.hacerfak.coreWMS.modules.expedicao.service;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusQualidade; // Importante
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoMovimento;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LocalizacaoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.EstoqueService;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.TarefaSeparacao;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.TarefaSeparacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PickingService {

        private final TarefaSeparacaoRepository tarefaRepository;
        private final EstoqueService estoqueService;
        private final EstoqueSaldoRepository saldoRepository;
        private final LocalizacaoRepository localizacaoRepository;

        @Transactional
        public void confirmarSeparacao(Long tarefaId, Long localDestinoId, String usuarioResponsavel) {
                TarefaSeparacao tarefa = tarefaRepository.findById(tarefaId)
                                .orElseThrow(() -> new EntityNotFoundException("Tarefa de separação não encontrada"));

                if (tarefa.getStatus() == StatusTarefa.CONCLUIDA) {
                        return; // Idempotência
                }

                Localizacao destino = localizacaoRepository.findById(localDestinoId)
                                .orElseThrow(() -> new EntityNotFoundException("Local de destino não encontrado"));

                // 1. Baixa a Reserva do Saldo na Origem
                // CORREÇÃO: Busca exata considerando que alocamos saldo DISPONIVEL
                EstoqueSaldo saldoOrigem = saldoRepository.buscarSaldoExato(
                                tarefa.getProduto().getId(),
                                tarefa.getOrigem().getId(),
                                null, // LPN
                                tarefa.getLoteSolicitado(),
                                null, // Serial
                                StatusQualidade.DISPONIVEL // <--- Especificamos a qualidade
                ).orElseThrow(() -> new EntityNotFoundException(
                                "Saldo de origem não encontrado ou divergente."));

                if (saldoOrigem.getQuantidadeReservada().compareTo(tarefa.getQuantidadePlanejada()) < 0) {
                        System.err.println("AVISO: Tentativa de baixar reserva maior que a existente. Ajustando.");
                        saldoOrigem.setQuantidadeReservada(BigDecimal.ZERO);
                } else {
                        saldoOrigem.setQuantidadeReservada(
                                        saldoOrigem.getQuantidadeReservada().subtract(tarefa.getQuantidadePlanejada()));
                }
                saldoRepository.save(saldoOrigem);

                // 2. Movimentação Física (SAÍDA da Origem)
                // CORREÇÃO: Adicionado StatusQualidade.DISPONIVEL
                estoqueService.movimentar(
                                tarefa.getProduto().getId(),
                                tarefa.getOrigem().getId(),
                                tarefa.getQuantidadePlanejada(),
                                null,
                                tarefa.getLoteSolicitado(),
                                null,
                                StatusQualidade.DISPONIVEL, // <--- AQUI
                                TipoMovimento.SAIDA,
                                usuarioResponsavel,
                                "Picking Onda " + tarefa.getOnda().getCodigo());

                // 3. Entrada no Destino (Doca/Stage)
                // CORREÇÃO: Adicionado StatusQualidade.DISPONIVEL
                estoqueService.movimentar(
                                tarefa.getProduto().getId(),
                                destino.getId(),
                                tarefa.getQuantidadePlanejada(),
                                null,
                                tarefa.getLoteSolicitado(),
                                null,
                                StatusQualidade.DISPONIVEL, // <--- AQUI
                                TipoMovimento.ENTRADA,
                                usuarioResponsavel,
                                "Stage Onda " + tarefa.getOnda().getCodigo());

                // 4. Conclui Tarefa
                tarefa.setDestino(destino);
                tarefa.setQuantidadeExecutada(tarefa.getQuantidadePlanejada());
                tarefa.concluir();
                tarefa.setUsuarioAtribuido(usuarioResponsavel);

                tarefaRepository.save(tarefa);
        }
}