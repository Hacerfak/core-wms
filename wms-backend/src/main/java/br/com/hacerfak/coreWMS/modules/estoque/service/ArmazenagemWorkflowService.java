package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.repository.*;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.SolicitacaoSaidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArmazenagemWorkflowService {

    private final LpnRepository lpnRepository;
    private final TarefaArmazenagemRepository tarefaRepository;
    private final LocalizacaoRepository localizacaoRepository;
    private final EstoqueService estoqueService;
    private final SolicitacaoSaidaRepository solicitacaoSaidaRepository;

    @Transactional
    public void gerarTarefasDeArmazenagem(Long solicitacaoEntradaId) {
        List<Lpn> lpnsPendentes = lpnRepository.findBySolicitacaoEntradaIdAndStatus(
                solicitacaoEntradaId, StatusLpn.FECHADO);

        // Busca Local de Expedição (Stage) para Cross-Docking
        Localizacao stageExpedicao = localizacaoRepository.findFirstByTipoAndAtivoTrue(TipoLocalizacao.DOCA)
                .orElse(null); // Ajustar TipoLocalizacao conforme seu Enum

        for (Lpn lpn : lpnsPendentes) {
            if (tarefaRepository.existsByLpnIdAndStatus(lpn.getId(), StatusTarefa.PENDENTE))
                continue;

            Localizacao destinoSugerido = null;

            // 1. Verifica Qualidade (Prioridade Máxima: Segregação)
            boolean temAvaria = lpn.getItens().stream()
                    .anyMatch(i -> i.getStatusQualidade() == StatusQualidade.AVARIA
                            || i.getStatusQualidade() == StatusQualidade.BLOQUEADO);

            if (temAvaria) {
                destinoSugerido = localizacaoRepository.findFirstByTipoAndAtivoTrue(TipoLocalizacao.AVARIA)
                        .orElse(null);
            }
            // 2. Verifica Cross-Docking (Se qualidade OK)
            else {
                // Se algum produto da LPN tem demanda de saída urgente
                boolean crossDockingCandidate = lpn.getItens().stream()
                        .anyMatch(item -> solicitacaoSaidaRepository
                                .existeDemandaPendenteParaProduto(item.getProduto().getId()));

                if (crossDockingCandidate && stageExpedicao != null) {
                    destinoSugerido = stageExpedicao;
                    // Opcional: Marcar na tarefa ou Log que é Cross-Docking
                    System.out.println("CROSS-DOCKING DETECTADO PARA LPN: " + lpn.getCodigo());
                }

                // 3. --- MELHORIA 2: SLOTTING INTELIGENTE ---
                if (destinoSugerido == null && !lpn.getItens().isEmpty()) {
                    // Pega o produto principal da LPN (assumindo mono-produto ou predominante)
                    Produto produtoPrincipal = lpn.getItens().get(0).getProduto();
                    destinoSugerido = definirMelhorEndereco(produtoPrincipal);
                }

            }

            // Se não for Avaria nem Cross-Docking, destinoSugerido fica null (sistema de
            // slotting depois define)

            TarefaArmazenagem tarefa = TarefaArmazenagem.builder()
                    .lpn(lpn)
                    .origem(lpn.getLocalizacaoAtual())
                    .solicitacaoEntradaId(solicitacaoEntradaId)
                    .destinoSugerido(destinoSugerido)
                    .build();

            tarefa.setStatus(StatusTarefa.PENDENTE);
            tarefaRepository.save(tarefa);
        }
    }

    private Localizacao definirMelhorEndereco(Produto produto) {
        // Estratégia 1: Consolidação (Encher locais que já têm esse produto)
        List<Localizacao> consolidacao = localizacaoRepository.findLocaisComProduto(produto.getId());
        if (!consolidacao.isEmpty()) {
            return consolidacao.get(0); // Retorna o primeiro encontrado
        }

        // Estratégia 2: Endereço Vazio (Busca o primeiro livre)
        List<Localizacao> vazios = localizacaoRepository.findLocaisVazios();
        if (!vazios.isEmpty()) {
            return vazios.get(0);
        }

        return null; // Sem sugestão (Operador decide)
    }

    /**
     * EXECUÇÃO: Operador confirma que guardou o pallet no endereço.
     */
    @Transactional
    public void confirmarArmazenagem(Long tarefaId, Long localDestinoId, String usuario) {
        TarefaArmazenagem tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));

        if (tarefa.getStatus() == StatusTarefa.CONCLUIDA) {
            throw new IllegalStateException("Tarefa já concluída.");
        }

        Localizacao destino = localizacaoRepository.findById(localDestinoId)
                .orElseThrow(() -> new EntityNotFoundException("Local de destino não encontrado"));

        Lpn lpn = tarefa.getLpn();

        // 1. Atualiza a LPN
        lpn.setLocalizacaoAtual(destino);
        lpn.setStatus(StatusLpn.ARMAZENADO); // Agora está disponível!
        lpnRepository.save(lpn);

        // 2. Atualiza/Cria os Saldos de Estoque (Item a Item)
        // Isso torna o estoque visível para a Expedição
        for (LpnItem item : lpn.getItens()) {
            atualizarSaldoFisico(item, destino, usuario);
        }

        // 3. Conclui a Tarefa
        tarefa.concluir(); // Seta status CONCLUIDA e data fim
        tarefa.setUsuarioAtribuido(usuario);
        tarefaRepository.save(tarefa);
    }

    private void atualizarSaldoFisico(LpnItem item, Localizacao local, String usuario) {
        estoqueService.movimentar(
                item.getProduto().getId(),
                local.getId(),
                item.getQuantidade(),
                item.getLpn().getCodigo(),
                item.getLote(),
                item.getNumeroSerie(), // <--- Passando o serial aqui
                item.getStatusQualidade(),
                TipoMovimento.ENTRADA,
                usuario,
                "Armazenagem LPN " + item.getLpn().getCodigo());
    }
}