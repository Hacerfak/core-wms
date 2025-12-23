package br.com.hacerfak.coreWMS.modules.expedicao.service;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusQualidade;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoMovimento;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LocalizacaoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.EstoqueService;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.TarefaSeparacao;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.TarefaSeparacaoRepository;
import br.com.hacerfak.coreWMS.modules.inventario.service.InventarioService; // Integração com Bloco 2
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
        private final InventarioService inventarioService; // <--- Novo

        @Transactional
        public void confirmarSeparacao(Long tarefaId, Long localDestinoId, BigDecimal qtdConfirmada,
                        String usuarioResponsavel) {
                TarefaSeparacao tarefa = tarefaRepository.findById(tarefaId)
                                .orElseThrow(() -> new EntityNotFoundException("Tarefa de separação não encontrada"));

                if (tarefa.getStatus() == StatusTarefa.CONCLUIDA)
                        return;

                Localizacao destino = localizacaoRepository.findById(localDestinoId)
                                .orElseThrow(() -> new EntityNotFoundException("Local de destino não encontrado"));

                // Se não informou quantidade, assume que pegou tudo (Picking Total)
                BigDecimal qtdExecutada = (qtdConfirmada != null) ? qtdConfirmada : tarefa.getQuantidadePlanejada();

                // Validação básica
                if (qtdExecutada.compareTo(tarefa.getQuantidadePlanejada()) > 0) {
                        throw new IllegalArgumentException("Não é permitido separar mais do que o planejado.");
                }

                // Verifica se houve Corte (Short Pick)
                boolean houveCorte = qtdExecutada.compareTo(tarefa.getQuantidadePlanejada()) < 0;

                // 1. Baixa a Reserva do Saldo na Origem
                // (Temos que baixar a reserva TOTAL planejada, pois a tarefa vai morrer,
                // seja ela atendida 100% ou com corte)
                EstoqueSaldo saldoOrigem = saldoRepository.buscarSaldoExato(
                                tarefa.getProduto().getId(),
                                tarefa.getOrigem().getId(),
                                null,
                                tarefa.getLoteSolicitado(),
                                null,
                                StatusQualidade.DISPONIVEL)
                                .orElseThrow(() -> new EntityNotFoundException("Saldo de origem não encontrado."));

                // Remove a reserva total da tarefa
                BigDecimal reservaParaRemover = tarefa.getQuantidadePlanejada();
                if (saldoOrigem.getQuantidadeReservada().compareTo(reservaParaRemover) < 0) {
                        // Ajuste defensivo caso a reserva esteja inconsistente
                        reservaParaRemover = saldoOrigem.getQuantidadeReservada();
                }
                saldoOrigem.setQuantidadeReservada(saldoOrigem.getQuantidadeReservada().subtract(reservaParaRemover));
                saldoRepository.save(saldoOrigem);

                // 2. Movimentação Física (SAÍDA da Origem) - Apenas o que foi PEGO
                if (qtdExecutada.compareTo(BigDecimal.ZERO) > 0) {
                        estoqueService.movimentar(
                                        tarefa.getProduto().getId(),
                                        tarefa.getOrigem().getId(),
                                        qtdExecutada,
                                        null,
                                        tarefa.getLoteSolicitado(),
                                        null,
                                        StatusQualidade.DISPONIVEL,
                                        TipoMovimento.SAIDA,
                                        usuarioResponsavel,
                                        "Picking Onda " + tarefa.getOnda().getCodigo());

                        // 3. Entrada no Destino (Doca/Stage)
                        estoqueService.movimentar(
                                        tarefa.getProduto().getId(),
                                        destino.getId(),
                                        qtdExecutada,
                                        null,
                                        tarefa.getLoteSolicitado(),
                                        null,
                                        StatusQualidade.DISPONIVEL,
                                        TipoMovimento.ENTRADA,
                                        usuarioResponsavel,
                                        "Stage Onda " + tarefa.getOnda().getCodigo());
                }

                // 4. Tratamento de Exceção (Gatilho de Auditoria)
                if (houveCorte) {
                        System.out.println("ALERTA: Corte de Picking detectado na tarefa " + tarefa.getId());

                        // Gera inventário automático para checar o endereço problemático
                        inventarioService.gerarInventarioPorDemanda(
                                        tarefa.getOrigem(),
                                        tarefa.getProduto(),
                                        "Divergência Picking Tarefa #" + tarefa.getId());

                        // Aqui poderíamos adicionar lógica para tentar re-alocar o saldo faltante de
                        // outro lugar
                        // (Atualizando a SolicitacaoSaida para pendente novamente), mas por enquanto
                        // vamos aceitar o corte.
                }

                // 5. Conclui Tarefa
                tarefa.setDestino(destino);
                tarefa.setQuantidadeExecutada(qtdExecutada);
                tarefa.concluir();
                tarefa.setUsuarioAtribuido(usuarioResponsavel);

                tarefaRepository.save(tarefa);
        }
}