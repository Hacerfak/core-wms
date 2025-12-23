package br.com.hacerfak.coreWMS.modules.operacao.service;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusSolicitacao;
import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException; // <--- O IMPORT QUE FALTAVA
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.LpnService;
import br.com.hacerfak.coreWMS.modules.operacao.domain.*;
import br.com.hacerfak.coreWMS.modules.operacao.dto.GerarLpnMassaRequest;
import br.com.hacerfak.coreWMS.modules.operacao.event.EntradaFinalizadaEvent;
import br.com.hacerfak.coreWMS.modules.operacao.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecebimentoWorkflowService {

        private final SolicitacaoEntradaRepository solicitacaoRepo;
        private final TarefaConferenciaRepository tarefaRepo;
        private final ProdutoRepository produtoRepository;
        private final LpnService lpnService;
        private final ApplicationEventPublisher eventPublisher;

        // 1. Início: Criar Solicitação (Chamado pelo NfeImportService)
        @Transactional
        public SolicitacaoEntrada iniciarProcessoEntrada(SolicitacaoEntrada novaSolicitacao) {
                novaSolicitacao.setStatus(StatusSolicitacao.CRIADA);
                SolicitacaoEntrada salva = solicitacaoRepo.save(novaSolicitacao);

                // Orquestração: Gera automaticamente a primeira tarefa
                gerarTarefaConferencia(salva);

                return salva;
        }

        private void gerarTarefaConferencia(SolicitacaoEntrada solicitacao) {
                // Regra de Negócio: Verifica se o parceiro exige conferência cega
                boolean isCega = solicitacao.getFornecedor().isRecebimentoCego();

                TarefaConferencia tarefa = TarefaConferencia.builder()
                                .solicitacaoPai(solicitacao)
                                .cega(isCega)
                                .build();

                // Define status inicial da tarefa
                tarefa.setStatus(StatusTarefa.PENDENTE);

                tarefaRepo.save(tarefa);

                // Atualiza status da solicitação pai
                solicitacao.setStatus(StatusSolicitacao.EM_PROCESSAMENTO);
                solicitacaoRepo.save(solicitacao);
        }

        // 2. Execução: Conferência em Massa (Monoproduto / Carga Fechada)
        @Transactional
        public List<String> realizarConferenciaEmMassa(GerarLpnMassaRequest dto, String usuario) {
                // 1. Validações
                SolicitacaoEntrada solicitacao = solicitacaoRepo.findById(dto.solicitacaoId())
                                .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada"));

                Produto produto = produtoRepository.findByCodigoBarras(dto.sku())
                                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));

                // Verifica se o produto pertence à nota (regra básica)
                ItemSolicitacaoEntrada itemSolicitacao = solicitacao.getItens().stream()
                                .filter(i -> i.getProduto().getId().equals(produto.getId()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Este produto não consta na Nota Fiscal."));

                // 2. Chama o LpnService para gerar os volumes físicos
                List<String> codigosLpn = lpnService.gerarLpnsComConteudo(
                                produto,
                                dto.quantidadePorVolume(),
                                dto.quantidadeDeVolumes(),
                                dto.lote(),
                                dto.dataValidade(),
                                usuario);

                // 3. Atualiza a contagem da Solicitação
                BigDecimal totalConferidoAgora = dto.quantidadePorVolume()
                                .multiply(new BigDecimal(dto.quantidadeDeVolumes()));

                itemSolicitacao.setQuantidadeConferida(
                                itemSolicitacao.getQuantidadeConferida().add(totalConferidoAgora));

                // Regra de negócio: Atualiza status da tarefa se finalizou tudo?
                // Por enquanto mantemos a tarefa aberta até o fechamento manual ou total.

                solicitacaoRepo.save(solicitacao);

                return codigosLpn; // Retorna para impressão
        }

        // 3. Execução: Operador finaliza a tarefa no coletor
        @Transactional
        public void concluirConferencia(Long tarefaId, String usuario) {
                TarefaConferencia tarefa = tarefaRepo.findById(tarefaId)
                                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));

                tarefa.concluir(); // Atualiza data fim e status
                tarefaRepo.save(tarefa);

                // Orquestração: O que acontece depois?
                // Se estiver tudo OK, finaliza a solicitação inteira
                finalizarSolicitacao(tarefa.getSolicitacaoPai(), usuario);
        }

        private void finalizarSolicitacao(SolicitacaoEntrada solicitacao, String usuario) {
                boolean temDivergencia = false;

                // Verifica item a item
                for (ItemSolicitacaoEntrada item : solicitacao.getItens()) {
                        int comp = item.getQuantidadeConferida().compareTo(item.getQuantidadePrevista());

                        if (comp < 0) {
                                // FALTA FÍSICA: Gerar Tarefa de Divergência
                                temDivergencia = true;
                                gerarTarefaDivergencia(solicitacao, item, TipoDivergencia.FALTA_FISICA,
                                                item.getQuantidadePrevista().subtract(item.getQuantidadeConferida()));

                        } else if (comp > 0) {
                                // SOBRA FÍSICA
                                temDivergencia = true;
                                gerarTarefaDivergencia(solicitacao, item, TipoDivergencia.SOBRA_FISICA,
                                                item.getQuantidadeConferida().subtract(item.getQuantidadePrevista()));
                        }
                }

                if (temDivergencia) {
                        // Se houve divergência, não conclui. Marca como PENDENTE_DIVERGENCIA
                        // O estoque físico (o que foi conferido) já entrou via LPN, mas o processo
                        // fiscal trava aqui.
                        solicitacao.setStatus(StatusSolicitacao.BLOQUEDA); // ou STATUS_DIVERGENTE
                } else {
                        solicitacao.setStatus(StatusSolicitacao.CONCLUIDA);
                }

                solicitacaoRepo.save(solicitacao);

                // Dispara evento para o Estoque (Armazenagem)
                // OBS: Mesmo com divergência de falta, o que FOI conferido precisa ser
                // armazenado.
                // Então disparamos o evento de qualquer forma para guardar os pallets físicos.
                eventPublisher.publishEvent(new EntradaFinalizadaEvent(
                                solicitacao.getId(),
                                TenantContext.getTenant(),
                                usuario));
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
                // tarefaDivergenciaRepo.save(tarefa); // Salvar no repo novo
        }
}