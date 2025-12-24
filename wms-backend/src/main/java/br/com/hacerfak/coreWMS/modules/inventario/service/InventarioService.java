package br.com.hacerfak.coreWMS.modules.inventario.service;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusQualidade;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoMovimento;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LocalizacaoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.service.EstoqueService;
import br.com.hacerfak.coreWMS.modules.inventario.domain.*;
import br.com.hacerfak.coreWMS.modules.inventario.dto.InventarioRequest;
import br.com.hacerfak.coreWMS.modules.inventario.repository.InventarioRepository;
import br.com.hacerfak.coreWMS.modules.inventario.repository.TarefaContagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final TarefaContagemRepository tarefaRepository;
    private final LocalizacaoRepository localizacaoRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueService estoqueService;
    private final EstoqueSaldoRepository saldoRepository;

    @Transactional
    public Inventario criarInventario(InventarioRequest dto) {
        Inventario inventario = Inventario.builder()
                .descricao(dto.descricao())
                .tipo(dto.tipo())
                .dataAgendada(dto.dataAgendada())
                .cego(dto.cego())
                .status(StatusInventario.ABERTO)
                .build();

        inventario = inventarioRepository.save(inventario);

        if (dto.localizacoesIds() != null) {
            for (Long locId : dto.localizacoesIds()) {
                gerarTarefaParaLocal(inventario, locId, null);
            }
        }
        return inventario;
    }

    @Transactional
    public void gerarInventarioPorDemanda(Localizacao local, Produto produto, String observacao) {
        Inventario inventario = Inventario.builder()
                .descricao("Auditoria Picking: " + observacao)
                .tipo(TipoInventario.POR_DEMANDA)
                .dataAgendada(LocalDate.now())
                .cego(true)
                .maxTentativas(2)
                .status(StatusInventario.EM_EXECUCAO)
                .build();

        inventario = inventarioRepository.save(inventario);
        gerarTarefaParaLocal(inventario, local.getId(), produto.getId());
    }

    private void gerarTarefaParaLocal(Inventario inv, Long localId, Long produtoId) {
        Localizacao loc = localizacaoRepository.findById(localId)
                .orElseThrow(() -> new EntityNotFoundException("Localização não encontrada: " + localId));

        // --- MELHORIA 1: BLOQUEIO AUTOMÁTICO ---
        if (!loc.isBloqueado()) {
            loc.setBloqueado(true);
            loc.setMotivoBloqueio("Inventário em Andamento (ID: " + inv.getId() + ")");
            localizacaoRepository.save(loc);
        }
        // ---------------------------------------

        Produto prod = produtoId != null ? produtoRepository.findById(produtoId).orElse(null) : null;
        BigDecimal snapshot;

        if (prod != null) {
            snapshot = saldoRepository.somarQuantidadePorLocalEProduto(localId, prod.getId());
        } else {
            snapshot = saldoRepository.somarQuantidadePorLocal(localId);
        }

        TarefaContagem tarefa = TarefaContagem.builder()
                .inventario(inv)
                .localizacao(loc)
                .produtoFoco(prod)
                .saldoSistemaSnapshot(snapshot)
                .build();

        tarefa.setStatus(StatusTarefa.PENDENTE);
        tarefaRepository.save(tarefa);
    }

    @Transactional
    public void registrarContagem(Long tarefaId, BigDecimal quantidade, String usuario) {
        TarefaContagem tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));

        if (tarefa.getQuantidadeContada1() == null) {
            tarefa.setQuantidadeContada1(quantidade);
            tarefa.setUsuarioContagem1(usuario);
        } else if (tarefa.getQuantidadeContada2() == null) {
            tarefa.setQuantidadeContada2(quantidade);
            tarefa.setUsuarioContagem2(usuario);
        } else if (tarefa.getQuantidadeContada3() == null) {
            tarefa.setQuantidadeContada3(quantidade);
            tarefa.setUsuarioContagem3(usuario);
            tarefa.setStatus(StatusTarefa.CONCLUIDA);
        }
        tarefaRepository.save(tarefa);
    }

    @Transactional
    public void finalizarInventario(Long inventarioId, String usuario) {
        Inventario inventario = inventarioRepository.findById(inventarioId)
                .orElseThrow(() -> new EntityNotFoundException("Inventário não encontrado"));

        Set<Long> locaisParaDesbloquear = new HashSet<>();

        for (TarefaContagem tarefa : inventario.getTarefas()) {
            locaisParaDesbloquear.add(tarefa.getLocalizacao().getId());

            BigDecimal qtdFinal = tarefa.getQuantidadeContada3();
            if (qtdFinal == null)
                qtdFinal = tarefa.getQuantidadeContada2();
            if (qtdFinal == null)
                qtdFinal = tarefa.getQuantidadeContada1();

            if (qtdFinal == null)
                continue;

            tarefa.setQuantidadeFinal(qtdFinal);
            BigDecimal diferenca = qtdFinal.subtract(tarefa.getSaldoSistemaSnapshot());

            if (diferenca.compareTo(BigDecimal.ZERO) != 0) {
                tarefa.setDivergente(true);
                if (tarefa.getProdutoFoco() != null) {
                    estoqueService.movimentar(
                            tarefa.getProdutoFoco().getId(),
                            tarefa.getLocalizacao().getId(),
                            diferenca.abs(),
                            null, null, null,
                            StatusQualidade.DISPONIVEL,
                            diferenca.compareTo(BigDecimal.ZERO) > 0 ? TipoMovimento.AJUSTE_POSITIVO
                                    : TipoMovimento.AJUSTE_NEGATIVO,
                            usuario,
                            "Inventário " + inventario.getId());
                }
            }
        }

        // --- MELHORIA 1: DESBLOQUEIO AUTOMÁTICO ---
        for (Long locId : locaisParaDesbloquear) {
            localizacaoRepository.findById(locId).ifPresent(loc -> {
                if (loc.isBloqueado() && loc.getMotivoBloqueio().contains(String.valueOf(inventarioId))) {
                    loc.setBloqueado(false);
                    loc.setMotivoBloqueio(null);
                    localizacaoRepository.save(loc);
                }
            });
        }
        // ------------------------------------------

        inventario.setStatus(StatusInventario.FINALIZADO);
        inventarioRepository.save(inventario);
    }
}