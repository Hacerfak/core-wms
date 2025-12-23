package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RessuprimentoService {

    private final ConfiguracaoPickingRepository configRepository;
    private final EstoqueSaldoRepository saldoRepository;
    private final TarefaMovimentacaoRepository tarefaRepository;
    private final EstoqueService estoqueService;
    private final LpnRepository lpnRepository;

    @Scheduled(fixedDelay = 600000) // 10 min
    @Transactional
    public void processarRessuprimento() {
        List<ConfiguracaoPicking> configs = configRepository.findByAtivoTrue();
        for (ConfiguracaoPicking config : configs) {
            try {
                verificarNecessidade(config);
            } catch (Exception e) {
                System.err.println("Erro ao processar ressuprimento: " + e.getMessage());
            }
        }
    }

    private void verificarNecessidade(ConfiguracaoPicking config) {
        if (tarefaRepository.existsByDestinoIdAndStatus(config.getLocalizacao().getId(), StatusTarefa.PENDENTE)) {
            return;
        }

        BigDecimal saldoAtual = saldoRepository.somarQuantidadePorLocalEProduto(
                config.getLocalizacao().getId(),
                config.getProduto().getId());
        if (saldoAtual == null)
            saldoAtual = BigDecimal.ZERO;

        if (saldoAtual.compareTo(config.getPontoRessuprimento()) <= 0) {
            gerarTarefaRessuprimento(config, saldoAtual);
        }
    }

    private void gerarTarefaRessuprimento(ConfiguracaoPicking config, BigDecimal saldoAtual) {
        BigDecimal quantidadeNecessaria = config.getCapacidadeMaxima().subtract(saldoAtual);
        if (quantidadeNecessaria.compareTo(BigDecimal.ZERO) <= 0)
            return;

        // Busca estoque no Pulmão (FIFO)
        List<EstoqueSaldo> candidatos = saldoRepository.buscarDisponiveisPorAntiguidade(config.getProduto().getId());

        EstoqueSaldo origem = candidatos.stream()
                .filter(s -> !s.getLocalizacao().getId().equals(config.getLocalizacao().getId()))
                .findFirst()
                .orElse(null);

        if (origem != null) {
            BigDecimal quantidadeMover = origem.getQuantidade().min(quantidadeNecessaria);

            // CORREÇÃO: Usamos apenas o código para referência, sem tentar setar ID nulo no
            // builder
            Lpn lpnRef = null;
            if (origem.getLpn() != null) {
                lpnRef = Lpn.builder().codigo(origem.getLpn()).build();
            }

            TarefaMovimentacao tarefa = TarefaMovimentacao.builder()
                    .tipoMovimento(TipoMovimentoInterno.RESSUPRIMENTO)
                    .produto(config.getProduto())
                    .origem(origem.getLocalizacao())
                    .destino(config.getLocalizacao())
                    .quantidade(quantidadeMover)
                    .lpn(lpnRef)
                    .build();

            tarefa.setStatus(StatusTarefa.PENDENTE);
            tarefaRepository.save(tarefa);
        }
    }

    // --- LÓGICA DE EXECUÇÃO CORRIGIDA ---

    @Transactional
    public void confirmarMovimentacao(Long tarefaId, String usuario) {
        TarefaMovimentacao tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));

        if (tarefa.getStatus() == StatusTarefa.CONCLUIDA)
            return;

        String codigoLpn = (tarefa.getLpn() != null) ? tarefa.getLpn().getCodigo() : null;
        boolean movendoLpnInteira = false;

        // 1. Verifica se estamos movendo a LPN inteira ou parcial
        if (codigoLpn != null) {
            // Busca o saldo ATUAL da LPN na origem para comparar
            Optional<EstoqueSaldo> saldoLpnOrigem = saldoRepository.buscarSaldoExato(
                    tarefa.getProduto().getId(),
                    tarefa.getOrigem().getId(),
                    codigoLpn,
                    null, null // Lote/Serial ignorados na busca simples, idealmente estariam na tarefa
            );

            if (saldoLpnOrigem.isPresent()) {
                // Se vou mover tudo o que tem lá (ou mais), é movimento total
                if (tarefa.getQuantidade().compareTo(saldoLpnOrigem.get().getQuantidade()) >= 0) {
                    movendoLpnInteira = true;
                }
            }
        }

        // 2. Movimentação de SAÍDA da Origem
        // Sempre sai da LPN (se houver)
        estoqueService.movimentar(
                tarefa.getProduto().getId(),
                tarefa.getOrigem().getId(),
                tarefa.getQuantidade(),
                codigoLpn,
                null, null, // Lote/Serial
                StatusQualidade.DISPONIVEL,
                TipoMovimento.SAIDA,
                usuario,
                "Ressuprimento Saída - Tarefa " + tarefaId);

        // 3. Movimentação de ENTRADA no Destino
        if (movendoLpnInteira) {
            // CENÁRIO A: Baixou o Pallet Inteiro
            // O saldo entra no destino COM A LPN
            estoqueService.movimentar(
                    tarefa.getProduto().getId(),
                    tarefa.getDestino().getId(),
                    tarefa.getQuantidade(),
                    codigoLpn, // LPN vai junto
                    null, null,
                    StatusQualidade.DISPONIVEL,
                    TipoMovimento.ENTRADA,
                    usuario,
                    "Ressuprimento (Full Pallet) - Tarefa " + tarefaId);

            // Atualiza a localização física da LPN na tabela tb_lpn
            Lpn lpnReal = lpnRepository.findByCodigo(codigoLpn)
                    .orElseThrow(() -> new EntityNotFoundException("LPN física não encontrada"));

            lpnReal.setLocalizacaoAtual(tarefa.getDestino());
            lpnRepository.save(lpnReal);

        } else {
            // CENÁRIO B: Picking Parcial
            // O saldo entra no destino SOLTO (Sem LPN)
            // A LPN original continua no pulmão (com saldo reduzido pela operação de Saída
            // acima)
            estoqueService.movimentar(
                    tarefa.getProduto().getId(),
                    tarefa.getDestino().getId(),
                    tarefa.getQuantidade(),
                    null, // <--- Entra sem LPN no picking
                    null, null,
                    StatusQualidade.DISPONIVEL,
                    TipoMovimento.ENTRADA,
                    usuario,
                    "Ressuprimento (Parcial) - Tarefa " + tarefaId);
        }

        tarefa.concluir();
        tarefa.setUsuarioAtribuido(usuario);
        tarefaRepository.save(tarefa);
    }
}