package br.com.hacerfak.coreWMS.modules.operacao.service;

import br.com.hacerfak.coreWMS.core.config.MessagingConfig;
import br.com.hacerfak.coreWMS.core.domain.workflow.StatusSolicitacao;
import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.EstoqueService;
import br.com.hacerfak.coreWMS.modules.estoque.service.LpnService;
import br.com.hacerfak.coreWMS.modules.operacao.domain.*;
import br.com.hacerfak.coreWMS.modules.operacao.dto.GerarLpnMassaRequest;
import br.com.hacerfak.coreWMS.modules.operacao.dto.ProgressoRecebimentoDTO;
import br.com.hacerfak.coreWMS.modules.operacao.event.EntradaCriadaEvent;
import br.com.hacerfak.coreWMS.modules.operacao.event.EntradaFinalizadaEvent;
import br.com.hacerfak.coreWMS.modules.faturamento.dto.FaturamentoEvent;
import br.com.hacerfak.coreWMS.modules.sistema.repository.SistemaConfigRepository;
import br.com.hacerfak.coreWMS.modules.operacao.repository.*;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Lpn;
import br.com.hacerfak.coreWMS.modules.estoque.domain.LpnItem;
import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusLpn;
import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusQualidade;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoMovimento;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LocalizacaoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnRepository;
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
        private final LpnRepository lpnRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final RabbitTemplate rabbitTemplate;
        private final TarefaDivergenciaRepository divergenciaRepository;
        private final ItemSolicitacaoEntradaRepository itemSolicitacaoRepo;
        private final SistemaConfigRepository sistemaConfigRepository;
        private final LocalizacaoRepository localizacaoRepository;
        private final EstoqueService estoqueService;

        // 1. Início: Criar Solicitação
        @Transactional
        public SolicitacaoEntrada iniciarProcessoEntrada(SolicitacaoEntrada novaSolicitacao) {
                novaSolicitacao.setStatus(StatusSolicitacao.CRIADA);
                SolicitacaoEntrada salva = solicitacaoRepo.save(novaSolicitacao);
                // REMOVIDO: gerarTarefaConferencia(salva);
                // MOTIVO: A tarefa só deve ser gerada após atribuir a Doca.

                // Dispara evento para processamento assíncrono (Arquitetura Event Driven)
                eventPublisher.publishEvent(new EntradaCriadaEvent(salva.getId(), TenantContext.getTenant()));

                return salva;
        }

        // Método chamado pelo Listener (Assíncrono)
        @Transactional
        public void processarInicioEntrada(Long solicitacaoId) {
                SolicitacaoEntrada solicitacao = solicitacaoRepo.findById(solicitacaoId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Solicitação não encontrada no processamento async"));

                boolean isCega = solicitacao.getFornecedor().isRecebimentoCego();

                TarefaConferencia tarefa = TarefaConferencia.builder()
                                .solicitacaoPai(solicitacao)
                                .cega(isCega)
                                .build();
                tarefa.setStatus(StatusTarefa.PENDENTE);
                tarefaRepo.save(tarefa);

                solicitacao.setStatus(StatusSolicitacao.EM_PROCESSAMENTO);
                solicitacaoRepo.save(solicitacao);
                log.info("Processamento async da solicitação #{} concluído. Tarefa gerada.", solicitacaoId);
        }

        private void gerarTarefaConferencia(SolicitacaoEntrada solicitacao, StatusTarefa statusInicial) {
                boolean isCega = solicitacao.getFornecedor().isRecebimentoCego();
                TarefaConferencia tarefa = TarefaConferencia.builder()
                                .solicitacaoPai(solicitacao)
                                .cega(isCega)
                                .build();
                tarefa.setStatus(statusInicial);
                tarefaRepo.save(tarefa);
        }

        // 2. Execução: Conferência em Massa
        @Transactional
        public List<String> realizarConferenciaEmMassa(GerarLpnMassaRequest dto, String usuario) {
                SolicitacaoEntrada solicitacao = solicitacaoRepo.findById(dto.solicitacaoId())
                                .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada"));

                Produto produto = produtoRepository.findByCodigoBarras(dto.sku())
                                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));

                // OTIMIZAÇÃO: Substituição de Stream por Loop for tradicional (Menor overhead
                // de memória)
                ItemSolicitacaoEntrada itemSolicitacao = null;
                if (solicitacao.getItens() != null) {
                        for (ItemSolicitacaoEntrada item : solicitacao.getItens()) {
                                if (item.getProduto().getId().equals(produto.getId())) {
                                        itemSolicitacao = item;
                                        break;
                                }
                        }
                }

                if (itemSolicitacao == null) {
                        throw new IllegalArgumentException("Este produto não consta na Nota Fiscal.");
                }

                // 1. VALIDAÇÃO DE DOCA
                if (solicitacao.getDoca() == null) {
                        throw new IllegalStateException(
                                        "A solicitação não possui uma Doca atribuída. Selecione a doca antes de conferir.");
                }

                // --- NOVA LÓGICA DE STATUS: INÍCIO DA OPERAÇÃO ---
                if (solicitacao.getStatus() == StatusSolicitacao.AGUARDANDO_EXECUCAO ||
                                solicitacao.getStatus() == StatusSolicitacao.CRIADA) {

                        solicitacao.setStatus(StatusSolicitacao.EM_PROCESSAMENTO);
                        solicitacaoRepo.save(solicitacao);

                        // Atualiza a tarefa vinculada
                        List<TarefaConferencia> tarefas = tarefaRepo.findBySolicitacaoPaiIdAndStatus(
                                        solicitacao.getId(), StatusTarefa.PENDENTE);

                        for (TarefaConferencia t : tarefas) {
                                t.iniciar(usuario); // Seta status EM_EXECUCAO (ou podemos mudar para EM_CONFERENCIA no
                                                    // enum)
                                t.setStatus(StatusTarefa.EM_EXECUCAO); // Força o enum específico
                                tarefaRepo.save(t);
                        }
                }

                // 1. Gera LPNs (Otimizado no LpnService)
                List<String> codigosLpn = lpnService.gerarLpnsComConteudo(
                                produto,
                                dto.quantidadePorVolume(),
                                dto.quantidadeDeVolumes(),
                                dto.lote(),
                                dto.dataValidade(),
                                dto.numeroSerie(),
                                solicitacao.getDoca(),
                                solicitacao.getId(),
                                usuario);

                // 2. OTIMIZAÇÃO: Atualiza a contagem no banco via SQL
                BigDecimal totalConferido = dto.quantidadePorVolume()
                                .multiply(new BigDecimal(dto.quantidadeDeVolumes()));

                itemSolicitacaoRepo.somarQuantidadeConferida(
                                dto.solicitacaoId(),
                                produto.getId(),
                                totalConferido);

                return codigosLpn;
        }

        // 3. Execução: Operador finaliza
        @Transactional
        public void concluirConferencia(Long tarefaId, Long stageId, String usuario) {
                TarefaConferencia tarefa = tarefaRepo.findById(tarefaId).orElseThrow();
                SolicitacaoEntrada solicitacao = tarefa.getSolicitacaoPai();
                Localizacao stage = localizacaoRepository.findById(stageId).orElseThrow();

                // 1. MOVER SALDO RESIDUAL (DOCA -> STAGE)
                // Busca todas as LPNs desta solicitação que AINDA estão na doca
                List<Lpn> lpnsNaDoca = lpnRepository.findBySolicitacaoEntradaIdAndLocalizacaoAtualId(
                                solicitacao.getId(),
                                solicitacao.getDoca().getId());

                log.info("Movendo {} LPNs da Doca {} para Stage {}", lpnsNaDoca.size(),
                                solicitacao.getDoca().getCodigo(), stage.getCodigo());

                for (Lpn lpn : lpnsNaDoca) {
                        // Mover fisicamente (Atualiza saldo e Localização da LPN)
                        // Aqui podemos usar um método de movimentação interna do EstoqueService
                        // Simulação da chamada:
                        estoqueService.transferirLpnInteira(
                                        lpn,
                                        stage,
                                        usuario,
                                        "Finalização Recebimento");
                }

                tarefa.concluir();
                tarefaRepo.save(tarefa);

                finalizarSolicitacao(solicitacao, usuario);
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
                        solicitacao.setStatus(StatusSolicitacao.DIVERGENTE);
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

        @Transactional(readOnly = true)
        public List<Lpn> listarLpnsPorSolicitacao(Long solicitacaoId) {
                // Traz LPNs vinculadas a esta solicitação.
                // O StatusLpn.FECHADO é o padrão pós-conferência, mas podemos trazer outros se
                // necessário.
                // Se quiser ver todas, remova o filtro de status no repository ou crie um
                // método novo findBySolicitacaoEntradaId.
                return lpnRepository.findBySolicitacaoEntradaIdAndStatus(solicitacaoId, StatusLpn.FECHADO);
        }

        // --- NOVA LÓGICA: RESETAR CONFERÊNCIA (Botão "Cancelar" na tela de
        // conferência) ---
        @Transactional
        public void resetarConferencia(Long id, String usuario) {
                SolicitacaoEntrada solicitacao = solicitacaoRepo.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada"));

                if (solicitacao.isConcluida()) {
                        throw new IllegalStateException("Recebimento já concluído não pode ser reiniciado.");
                }

                // 1. Validação de Estoque
                boolean temArmazenado = lpnRepository.findBySolicitacaoEntradaIdAndStatus(id, StatusLpn.ARMAZENADO)
                                .size() > 0;
                if (temArmazenado) {
                        throw new IllegalStateException("Existem volumes já armazenados. Realize o estorno manual.");
                }

                // 2. Limpeza de LPNs na Doca
                List<Lpn> lpns = lpnRepository.findBySolicitacaoEntradaIdAndStatus(id, StatusLpn.FECHADO);
                lpns.addAll(lpnRepository.findBySolicitacaoEntradaIdAndStatus(id, StatusLpn.EM_MONTAGEM));

                for (Lpn lpn : lpns) {
                        if (lpn.getLocalizacaoAtual() != null) {
                                estoqueService.movimentar(
                                                lpn.getItens().get(0).getProduto().getId(),
                                                lpn.getLocalizacaoAtual().getId(),
                                                lpn.getItens().get(0).getQuantidade(),
                                                lpn.getCodigo(), lpn.getItens().get(0).getLote(),
                                                lpn.getItens().get(0).getNumeroSerie(),
                                                StatusQualidade.DISPONIVEL, TipoMovimento.AJUSTE_NEGATIVO, usuario,
                                                "Reset Conferência");
                        }
                        lpnRepository.delete(lpn);
                }

                // 3. Zera Contagem dos Itens
                for (ItemSolicitacaoEntrada item : solicitacao.getItens()) {
                        item.setQuantidadeConferida(BigDecimal.ZERO);
                        itemSolicitacaoRepo.save(item);
                }

                // 4. REINICIA A TAREFA (Sem duplicar)
                TarefaConferencia tarefa = tarefaRepo.findBySolicitacaoPaiId(id).stream()
                                .filter(t -> t.getStatus() != StatusTarefa.CANCELADA)
                                .findFirst()
                                .orElse(null);

                if (tarefa != null) {
                        // Reseta a tarefa existente
                        tarefa.setStatus(StatusTarefa.PENDENTE);
                        tarefa.setUsuarioAtribuido(null); // Libera para qualquer um pegar
                        tarefa.setInicioExecucao(null);
                        tarefa.setFimExecucao(null);
                        tarefaRepo.save(tarefa);
                } else {
                        // Cria se não existir (caso de erro anterior)
                        gerarTarefaConferencia(solicitacao, StatusTarefa.PENDENTE);
                }

                solicitacao.setStatus(StatusSolicitacao.AGUARDANDO_EXECUCAO);
                solicitacaoRepo.save(solicitacao);

                log.info("Conferência {} reiniciada por {}", id, usuario);
        }

        @Transactional
        public void estornarLpn(Long solicitacaoId, Long lpnId, String usuario) {
                SolicitacaoEntrada sol = solicitacaoRepo.findById(solicitacaoId)
                                .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada"));

                if (sol.isConcluida()) {
                        throw new IllegalStateException("Não é possível estornar LPN de recebimento já finalizado.");
                }

                Lpn lpn = lpnRepository.findById(lpnId)
                                .orElseThrow(() -> new EntityNotFoundException("LPN não encontrada"));

                // Valida se a LPN pertence mesmo a essa solicitação
                if (lpn.getSolicitacaoEntradaId() == null || !lpn.getSolicitacaoEntradaId().equals(solicitacaoId)) {
                        throw new IllegalArgumentException("Esta LPN não pertence à solicitação informada.");
                }

                // VALIDAÇÃO DE LOCALIZAÇÃO (TRAVA DE SEGURANÇA)
                if (!lpn.getLocalizacaoAtual().getId().equals(sol.getDoca().getId())) {
                        throw new IllegalStateException(String.format(
                                        "A LPN %s já foi movimentada para fora da doca (%s). Realize a devolução física para a doca antes de estornar.",
                                        lpn.getCodigo(), lpn.getLocalizacaoAtual().getEnderecoCompleto()));
                }

                // Valida se a LPN já foi armazenada (se sim, não pode apenas deletar, tem que
                // mover estoque)
                if (lpn.getStatus() == StatusLpn.ARMAZENADO || lpn.getStatus() == StatusLpn.EXPEDIDO) {
                        throw new IllegalStateException(
                                        "LPN já processada no estoque físico. Faça uma movimentação de saída/bloqueio.");
                }

                // 1. OTIMIZAÇÃO: Reverte via SQL (evita carregar listas gigantes)
                for (LpnItem lpnItem : lpn.getItens()) {
                        itemSolicitacaoRepo.subtrairQuantidadeConferida(
                                        solicitacaoId,
                                        lpnItem.getProduto().getId(),
                                        lpnItem.getQuantidade());
                }

                // 2. APAGAR SALDO DE ESTOQUE DA DOCA
                // Antes de deletar a LPN, precisamos remover o saldo que criamos na Doca.
                // Se já foi movido (validado acima), o saldo não estaria mais lá.
                // Como validamos que está na doca, podemos apagar.
                estoqueService.movimentar(
                                lpn.getItens().get(0).getProduto().getId(),
                                lpn.getLocalizacaoAtual().getId(),
                                lpn.getItens().get(0).getQuantidade(), // Assume monoproduto por enquanto ou loop
                                lpn.getCodigo(),
                                lpn.getItens().get(0).getLote(),
                                null, // serial
                                StatusQualidade.DISPONIVEL,
                                TipoMovimento.AJUSTE_NEGATIVO, // Ou um tipo ESTORNO_RECEBIMENTO
                                usuario,
                                "Estorno de Recebimento");

                // 2. Deleta LPN
                lpnRepository.delete(lpn);
                log.info("LPN {} estornada por {}", lpn.getCodigo(), usuario);
        }

        @Transactional
        public void resolverDivergencia(Long divergenciaId, boolean aceitar, String observacao, String usuario) {
                TarefaDivergencia tarefa = divergenciaRepository.findById(divergenciaId)
                                .orElseThrow(() -> new EntityNotFoundException("Divergência não encontrada"));

                if (tarefa.getStatus() != StatusTarefa.PENDENTE) {
                        throw new IllegalStateException("Esta divergência já foi tratada.");
                }

                SolicitacaoEntrada solicitacao = tarefa.getSolicitacao();

                if (aceitar) {
                        // Lógica de ACEITE:
                        // O sistema entende que a contagem física (mesmo diferente da nota) é a
                        // verdade.
                        // A diferença já foi registrada no estoque físico durante a conferência (via
                        // LPNs).
                        // Apenas registramos a resolução.
                        tarefa.setResolucao("ACEITE: " + observacao);
                        tarefa.concluir();
                } else {
                        // Lógica de RECUSA:
                        // Aqui é complexo. Se recusar uma SOBRA, teríamos que bloquear o lote ou gerar
                        // devolução.
                        // Se recusar uma FALTA, implica que o fornecedor vai mandar depois?
                        // Para este MVP, vamos considerar que "Não Aceitar" apenas registra a disputa,
                        // mas libera a nota para não travar o depósito.
                        // Num cenário real, geraria uma Nota de Devolução.
                        tarefa.setResolucao("DISPUTA/RECUSA: " + observacao);
                        tarefa.concluir();
                }

                tarefa.setUsuarioAtribuido(usuario);
                divergenciaRepository.save(tarefa);

                // Verifica se ainda existem divergências pendentes para esta solicitação
                boolean temPendencia = divergenciaRepository.findBySolicitacaoId(solicitacao.getId())
                                .stream()
                                .anyMatch(t -> t.getStatus() == StatusTarefa.PENDENTE);

                if (!temPendencia) {
                        // Se resolveu tudo, libera a solicitação
                        solicitacao.setStatus(StatusSolicitacao.CONCLUIDA);
                        solicitacaoRepo.save(solicitacao);

                        // Re-dispara eventos de finalização (Integração/Faturamento)
                        // (Copiar a lógica do finalizarSolicitacao ou extrair para método comum)
                        log.info("Solicitação {} liberada após resolução de divergências.",
                                        solicitacao.getCodigoExterno());
                }
        }

        // Método para o Controller chamar o progresso rápido
        public ProgressoRecebimentoDTO getProgresso(Long id) {
                return solicitacaoRepo.buscarProgresso(id)
                                .orElse(new ProgressoRecebimentoDTO(id, BigDecimal.ZERO, BigDecimal.ZERO));
        }

        @Transactional(readOnly = true)
        public boolean isConferenciaCega(Long solicitacaoId) {
                // 1. Verifica Regra Global da Empresa
                boolean globalObrigatorio = sistemaConfigRepository.findById("RECEBIMENTO_CEGO_OBRIGATORIO")
                                .map(c -> Boolean.parseBoolean(c.getValor()))
                                .orElse(false);

                if (globalObrigatorio) {
                        return true; // Se a empresa obriga, ignora o parceiro
                }

                // 2. Verifica Regra Específica do Parceiro
                SolicitacaoEntrada solicitacao = solicitacaoRepo.findById(solicitacaoId)
                                .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada"));

                if (solicitacao.getFornecedor() != null) {
                        return solicitacao.getFornecedor().isRecebimentoCego();
                }

                return false; // Default: Aberta
        }

        @Transactional
        public void atribuirDoca(Long solicitacaoId, Long docaId) {
                SolicitacaoEntrada sol = solicitacaoRepo.findById(solicitacaoId)
                                .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada"));

                if (sol.isConcluida()) {
                        throw new IllegalStateException("Não é possível alterar doca de solicitação concluída.");
                }

                Localizacao doca = localizacaoRepository.findById(docaId)
                                .orElseThrow(() -> new EntityNotFoundException("Doca não encontrada"));

                sol.setDoca(doca);
                sol.setStatus(StatusSolicitacao.AGUARDANDO_EXECUCAO);
                solicitacaoRepo.save(sol);

                // --- LÓGICA DE TAREFA ÚNICA ---
                // Busca se JÁ EXISTE alguma tarefa para esta solicitação que não esteja
                // cancelada
                TarefaConferencia tarefaExistente = tarefaRepo.findBySolicitacaoPaiId(sol.getId()).stream()
                                .filter(t -> t.getStatus() != StatusTarefa.CANCELADA)
                                .findFirst()
                                .orElse(null);

                if (tarefaExistente != null) {
                        // Se já existe, apenas atualiza o status para AGUARDANDO (se não estiver
                        // finalizada)
                        if (tarefaExistente.getStatus() != StatusTarefa.CONCLUIDA) {
                                tarefaExistente.setStatus(StatusTarefa.PENDENTE);
                                tarefaRepo.save(tarefaExistente);
                        }
                } else {
                        // Só cria se realmente não existir nenhuma
                        gerarTarefaConferencia(sol, StatusTarefa.PENDENTE);
                }
        }

        @Transactional
        public void cancelarRecebimento(Long id, String usuario) {
                SolicitacaoEntrada solicitacao = solicitacaoRepo.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada"));

                // 1. Validação: Não pode cancelar se já foi concluída
                if (solicitacao.getStatus() == StatusSolicitacao.CONCLUIDA) {
                        throw new IllegalStateException(
                                        "Não é possível cancelar um recebimento já concluído (Estoque consolidado).");
                }

                // 2. Verifica se existem LPNs geradas e se elas já foram armazenadas
                List<Lpn> lpns = lpnRepository.findBySolicitacaoEntradaIdAndStatus(id, StatusLpn.ARMAZENADO);
                if (!lpns.isEmpty()) {
                        throw new IllegalStateException(
                                        "Existem LPNs já armazenadas no estoque. Faça o estorno individual ou saída manual antes de cancelar.");
                }

                // 3. Limpeza de LPNs na Doca (Estorno em massa)
                // Se houver LPNs na Doca (não armazenadas), podemos apagar
                List<Lpn> lpnsNaDoca = lpnRepository.findBySolicitacaoEntradaIdAndStatus(id, StatusLpn.FECHADO);
                lpnsNaDoca.addAll(lpnRepository.findBySolicitacaoEntradaIdAndStatus(id, StatusLpn.EM_MONTAGEM));

                for (Lpn lpn : lpnsNaDoca) {
                        // Remove saldo da doca
                        if (lpn.getLocalizacaoAtual() != null) {
                                estoqueService.movimentar(
                                                lpn.getItens().get(0).getProduto().getId(),
                                                lpn.getLocalizacaoAtual().getId(),
                                                lpn.getItens().get(0).getQuantidade(),
                                                lpn.getCodigo(),
                                                lpn.getItens().get(0).getLote(),
                                                null,
                                                StatusQualidade.DISPONIVEL,
                                                TipoMovimento.AJUSTE_NEGATIVO,
                                                usuario,
                                                "Cancelamento de Recebimento");
                        }
                        lpnRepository.delete(lpn);
                }

                // 4. Cancelar Tarefas
                List<TarefaConferencia> tarefas = tarefaRepo.findBySolicitacaoPaiIdAndStatus(id, StatusTarefa.PENDENTE);
                tarefas.addAll(tarefaRepo.findBySolicitacaoPaiIdAndStatus(id, StatusTarefa.EM_EXECUCAO));

                tarefas.forEach(t -> {
                        t.setStatus(StatusTarefa.CANCELADA);
                        tarefaRepo.save(t);
                });

                // 5. Atualiza Status da Solicitação
                solicitacao.setStatus(StatusSolicitacao.CANCELADA);
                solicitacaoRepo.save(solicitacao);

                log.info("Recebimento {} cancelado por {}", id, usuario);
        }

        @Transactional
        public void excluirSolicitacao(Long id, String usuario) {
                SolicitacaoEntrada solicitacao = solicitacaoRepo.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Solicitação não encontrada"));

                // Verifica estoque consolidado
                boolean temEstoque = !lpnRepository.findBySolicitacaoEntradaIdAndStatus(id, StatusLpn.ARMAZENADO)
                                .isEmpty();

                if (temEstoque) {
                        // Apenas cancela
                        solicitacao.setStatus(StatusSolicitacao.CANCELADA);
                        // Cancela a tarefa associada
                        tarefaRepo.findBySolicitacaoPaiId(id).forEach(t -> {
                                t.setStatus(StatusTarefa.CANCELADA);
                                tarefaRepo.save(t);
                        });
                        solicitacaoRepo.save(solicitacao);
                } else {
                        // Remoção Física
                        // 1. Limpa LPNs da doca e saldos
                        List<Lpn> lpnsSujas = lpnRepository.findBySolicitacaoEntradaIdAndStatus(id, StatusLpn.FECHADO);
                        lpnsSujas.addAll(lpnRepository.findBySolicitacaoEntradaIdAndStatus(id, StatusLpn.EM_MONTAGEM));

                        for (Lpn l : lpnsSujas) {
                                if (l.getLocalizacaoAtual() != null) {
                                        estoqueService.movimentar(
                                                        l.getItens().get(0).getProduto().getId(),
                                                        l.getLocalizacaoAtual().getId(),
                                                        l.getItens().get(0).getQuantidade(), l.getCodigo(), null, null,
                                                        StatusQualidade.DISPONIVEL, TipoMovimento.AJUSTE_NEGATIVO,
                                                        usuario, "Exclusão");
                                }
                                lpnRepository.delete(l);
                        }

                        // 2. Remove Tarefas antes de remover a solicitação (se não for Cascade)
                        // Se estiver cascade no @OneToMany da Solicitação, o delete da solicitação
                        // resolve.
                        // Por segurança:
                        List<TarefaConferencia> tarefas = tarefaRepo.findBySolicitacaoPaiId(id);
                        tarefaRepo.deleteAll(tarefas);

                        solicitacaoRepo.delete(solicitacao);
                }
        }
}