package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.repository.*;
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

    @Transactional
    public void gerarTarefasDeArmazenagem(Long solicitacaoEntradaId) {
        List<Lpn> lpnsPendentes = lpnRepository.findBySolicitacaoEntradaIdAndStatus(
                solicitacaoEntradaId, StatusLpn.FECHADO);

        for (Lpn lpn : lpnsPendentes) {
            boolean jaTemTarefa = tarefaRepository.existsByLpnIdAndStatus(lpn.getId(), StatusTarefa.PENDENTE);
            if (jaTemTarefa)
                continue;

            // --- LÓGICA DE SEGREGAÇÃO AUTOMÁTICA ---
            Localizacao destinoSugerido = null;

            // Verifica se tem algum item avariado ou bloqueado dentro da LPN
            boolean temAvaria = lpn.getItens().stream()
                    .anyMatch(i -> i.getStatusQualidade() == StatusQualidade.AVARIA
                            || i.getStatusQualidade() == StatusQualidade.BLOQUEADO);

            if (temAvaria) {
                // Busca uma área de SEGREGACAO ou AVARIA no armazém
                // Simplificação: Pega o primeiro local do tipo AVARIA
                destinoSugerido = localizacaoRepository.findFirstByTipoAndAtivoTrue(TipoLocalizacao.AVARIA)
                        .orElse(null);
            }
            // ----------------------------------------

            TarefaArmazenagem tarefa = TarefaArmazenagem.builder()
                    .lpn(lpn)
                    .origem(lpn.getLocalizacaoAtual())
                    .solicitacaoEntradaId(solicitacaoEntradaId)
                    .destinoSugerido(destinoSugerido) // Salva a sugestão
                    .build();

            tarefa.setStatus(StatusTarefa.PENDENTE);
            tarefaRepository.save(tarefa);
        }
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