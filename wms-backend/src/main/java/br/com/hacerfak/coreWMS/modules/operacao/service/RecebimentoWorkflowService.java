package br.com.hacerfak.coreWMS.modules.operacao.service;

import br.com.hacerfak.coreWMS.core.config.MessagingConfig;
import br.com.hacerfak.coreWMS.core.domain.workflow.StatusSolicitacao;
import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.LpnService;
import br.com.hacerfak.coreWMS.modules.operacao.domain.*;
import br.com.hacerfak.coreWMS.modules.operacao.dto.GerarLpnMassaRequest;
import br.com.hacerfak.coreWMS.modules.operacao.event.EntradaFinalizadaEvent;
import br.com.hacerfak.coreWMS.modules.faturamento.dto.FaturamentoEvent; // Importante
import br.com.hacerfak.coreWMS.modules.operacao.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Adicionado para logs

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // Adicionado Logger
public class RecebimentoWorkflowService {

        private final SolicitacaoEntradaRepository solicitacaoRepo;
        private final TarefaConferenciaRepository tarefaRepo;
        private final ProdutoRepository produtoRepository;
        private final LpnService lpnService;
        private final ApplicationEventPublisher eventPublisher;
        private final RabbitTemplate rabbitTemplate;

        // 1. Início: Criar Solicitação
        @Transactional
        public SolicitacaoEntrada iniciarProcessoEntrada(SolicitacaoEntrada novaSolicitacao) {
                novaSolicitacao.setStatus(StatusSolicitacao.CRIADA);
                SolicitacaoEntrada salva = solicitacaoRepo.save(novaSolicitacao);
                gerarTarefaConferencia(salva);
                return salva;
        }

        private void gerarTarefaConferencia(SolicitacaoEntrada solicitacao) {
                boolean isCega = solicitacao.getFornecedor().isRecebimentoCego();
                TarefaConferencia tarefa = TarefaConferencia.builder()
                                .solicitacaoPai(solicitacao)
                                .cega(isCega)
                                .build();
                tarefa.setStatus(StatusTarefa.PENDENTE);
                tarefaRepo.save(tarefa);

                solicitacao.setStatus(StatusSolicitacao.EM_PROCESSAMENTO);
                solicitacaoRepo.save(solicitacao);
        }

        // 2. Execução: Conferência em Massa
        @Transactional
        public List<String> realizarConferenciaEmMassa(GerarLpnMassaRequest dto, String usuario) {
                SolicitacaoEntrada solicitacao = solicitacaoRepo.findById(dto.solicitacaoId())
                                .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada"));

                Produto produto = produtoRepository.findByCodigoBarras(dto.sku())
                                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));

                ItemSolicitacaoEntrada itemSolicitacao = solicitacao.getItens().stream()
                                .filter(i -> i.getProduto().getId().equals(produto.getId()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Este produto não consta na Nota Fiscal."));

                List<String> codigosLpn = lpnService.gerarLpnsComConteudo(
                                produto,
                                dto.quantidadePorVolume(),
                                dto.quantidadeDeVolumes(),
                                dto.lote(),
                                dto.dataValidade(),
                                usuario);

                BigDecimal totalConferidoAgora = dto.quantidadePorVolume()
                                .multiply(new BigDecimal(dto.quantidadeDeVolumes()));

                itemSolicitacao.setQuantidadeConferida(
                                itemSolicitacao.getQuantidadeConferida().add(totalConferidoAgora));

                solicitacaoRepo.save(solicitacao);
                return codigosLpn;
        }

        // 3. Execução: Operador finaliza
        @Transactional
        public void concluirConferencia(Long tarefaId, String usuario) {
                TarefaConferencia tarefa = tarefaRepo.findById(tarefaId)
                                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));

                tarefa.concluir();
                tarefaRepo.save(tarefa);

                finalizarSolicitacao(tarefa.getSolicitacaoPai(), usuario);
        }

        // --- MÉTODO CORRIGIDO ---
        private void finalizarSolicitacao(SolicitacaoEntrada solicitacao, String usuario) {
                boolean temDivergencia = false;

                for (ItemSolicitacaoEntrada item : solicitacao.getItens()) {
                        int comp = item.getQuantidadeConferida().compareTo(item.getQuantidadePrevista());

                        if (comp < 0) {
                                temDivergencia = true;
                                gerarTarefaDivergencia(solicitacao, item, TipoDivergencia.FALTA_FISICA,
                                                item.getQuantidadePrevista().subtract(item.getQuantidadeConferida()));

                        } else if (comp > 0) {
                                temDivergencia = true;
                                gerarTarefaDivergencia(solicitacao, item, TipoDivergencia.SOBRA_FISICA,
                                                item.getQuantidadeConferida().subtract(item.getQuantidadePrevista()));
                        }
                }

                if (temDivergencia) {
                        solicitacao.setStatus(StatusSolicitacao.BLOQUEDA);
                } else {
                        solicitacao.setStatus(StatusSolicitacao.CONCLUIDA);
                }

                solicitacaoRepo.save(solicitacao);

                // Captura o Tenant ATUAL para os eventos assíncronos
                String tenantAtual = TenantContext.getTenant();

                // Evento interno (Spring Events - Síncrono ou Async local)
                eventPublisher.publishEvent(new EntradaFinalizadaEvent(
                                solicitacao.getId(),
                                tenantAtual,
                                solicitacao,
                                solicitacao.getNotaFiscal(),
                                usuario));

                // --- MUDANÇA AQUI: Envia FaturamentoEvent para o RabbitMQ ---
                // Isso garante que o consumidor saiba qual Tenant usar.
                FaturamentoEvent eventoFaturamento = new FaturamentoEvent(solicitacao.getId(), tenantAtual);

                try {
                        rabbitTemplate.convertAndSend(MessagingConfig.QUEUE_FATURAMENTO, eventoFaturamento);
                        log.info("Faturamento solicitado via fila para Solicitação {} no Tenant {}",
                                        solicitacao.getId(), tenantAtual);
                } catch (Exception e) {
                        log.error("Falha ao enviar evento de faturamento para fila", e);
                        // Não damos throw para não rollbackar a transação do recebimento,
                        // pois o recebimento físico já aconteceu.
                }
        }

        private void gerarTarefaDivergencia(SolicitacaoEntrada sol, ItemSolicitacaoEntrada item,
                        TipoDivergencia tipo, BigDecimal qtd) {
                TarefaDivergencia tarefa = TarefaDivergencia.builder()
                                .solicitacao(sol)
                                .produto(item.getProduto())
                                .tipo(tipo)
                                .quantidadeDivergente(qtd)
                                .build();
                tarefa.setStatus(StatusTarefa.PENDENTE);
                // tarefaDivergenciaRepo.save(tarefa);
        }
}