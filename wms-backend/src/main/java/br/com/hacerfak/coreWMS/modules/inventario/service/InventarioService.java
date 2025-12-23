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

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final TarefaContagemRepository tarefaRepository;
    private final LocalizacaoRepository localizacaoRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueService estoqueService;
    private final EstoqueSaldoRepository saldoRepository;

    // 1. CRIAR INVENTÁRIO (Planejamento)
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

        // Gerar Tarefas (Simplificado: Gera para locais indicados)
        if (dto.localizacoesIds() != null) {
            for (Long locId : dto.localizacoesIds()) {
                gerarTarefaParaLocal(inventario, locId, null);
            }
        }
        // Lógica para produtos (Rotativo) seria iterar sobre locais onde o produto
        // existe

        return inventario;
    }

    /**
     * GATILHO DE EXCEÇÃO: Chamado automaticamente quando ocorre um erro de picking
     * (Short Pick).
     * Gera um inventário pontual para verificar o saldo do local.
     */
    @Transactional
    public void gerarInventarioPorDemanda(Localizacao local, Produto produto, String observacao) {
        // 1. Cria o cabeçalho do inventário
        Inventario inventario = Inventario.builder()
                .descricao("Auditoria Picking: " + observacao)
                .tipo(TipoInventario.POR_DEMANDA)
                .dataAgendada(LocalDate.now())
                .cego(true)
                .maxTentativas(2) // 2 tentativas para ser rápido
                .status(StatusInventario.EM_EXECUCAO) // Já nasce pronto para execução
                .build();

        inventario = inventarioRepository.save(inventario);

        // 2. Gera a tarefa de contagem (reutilizando lógica interna)
        // Precisamos expor ou duplicar a lógica de 'gerarTarefaParaLocal' se ela for
        // privada.
        // Vamos chamá-la diretamente aqui:
        gerarTarefaParaLocal(inventario, local.getId(), produto.getId());

        System.out.println("Gatilho de Inventário gerado: " + inventario.getDescricao());
    }

    private void gerarTarefaParaLocal(Inventario inv, Long localId, Long produtoId) {
        Localizacao loc = localizacaoRepository.findById(localId)
                .orElseThrow(() -> new EntityNotFoundException("Localização não encontrada: " + localId));

        Produto prod = produtoId != null ? produtoRepository.findById(produtoId).orElse(null) : null;

        BigDecimal snapshot;

        if (prod != null) {
            // Se a tarefa é focada em um produto, somamos apenas ele naquele local
            snapshot = saldoRepository.somarQuantidadePorLocalEProduto(localId, prod.getId());
        } else {
            // Se a tarefa é genérica para o local, somamos tudo o que existe lá
            snapshot = saldoRepository.somarQuantidadePorLocal(localId);
        }
        // ----------------------------------------------

        TarefaContagem tarefa = TarefaContagem.builder()
                .inventario(inv)
                .localizacao(loc)
                .produtoFoco(prod)
                .saldoSistemaSnapshot(snapshot) // Valor real agora!
                .build();

        tarefa.setStatus(StatusTarefa.PENDENTE);
        tarefaRepository.save(tarefa);
    }

    // 2. REGISTRAR CONTAGEM (Coletor)
    @Transactional
    public void registrarContagem(Long tarefaId, BigDecimal quantidade, String usuario) {
        TarefaContagem tarefa = tarefaRepository.findById(tarefaId)
                .orElseThrow(() -> new EntityNotFoundException("Tarefa não encontrada"));

        // Lógica simples: Preenche o primeiro slot vazio
        if (tarefa.getQuantidadeContada1() == null) {
            tarefa.setQuantidadeContada1(quantidade);
            tarefa.setUsuarioContagem1(usuario);
            // Verifica divergência imediata se não for cego (opcional)
        } else if (tarefa.getQuantidadeContada2() == null) {
            tarefa.setQuantidadeContada2(quantidade);
            tarefa.setUsuarioContagem2(usuario);
        } else if (tarefa.getQuantidadeContada3() == null) {
            tarefa.setQuantidadeContada3(quantidade);
            tarefa.setUsuarioContagem3(usuario);
            tarefa.setStatus(StatusTarefa.CONCLUIDA); // Encerra após 3 tentativas
        }

        tarefaRepository.save(tarefa);
    }

    // 3. FINALIZAR E AJUSTAR ESTOQUE (Supervisor)
    @Transactional
    public void finalizarInventario(Long inventarioId, String usuario) {
        Inventario inventario = inventarioRepository.findById(inventarioId)
                .orElseThrow(() -> new EntityNotFoundException("Inventário não encontrado"));

        for (TarefaContagem tarefa : inventario.getTarefas()) {
            // Define qual contagem vale (A última realizada ou média - regra de negócio)
            // Aqui assumimos a última preenchida como a verdadeira
            BigDecimal qtdFinal = tarefa.getQuantidadeContada3();
            if (qtdFinal == null)
                qtdFinal = tarefa.getQuantidadeContada2();
            if (qtdFinal == null)
                qtdFinal = tarefa.getQuantidadeContada1();

            if (qtdFinal == null)
                continue; // Ninguém contou

            tarefa.setQuantidadeFinal(qtdFinal);

            // Calcula diferença (Delta)
            BigDecimal saldoSistema = tarefa.getSaldoSistemaSnapshot(); // Usar snapshot ou saldo atual?
            // Para ajuste, o ideal é comparar com o saldo ATUAL do banco para aplicar o
            // delta,
            // mas cuidado com movimentações durante o inventário.
            // Vamos assumir inventário com operação parada (Wall-to-wall) para simplificar.

            BigDecimal diferenca = qtdFinal.subtract(saldoSistema);

            if (diferenca.compareTo(BigDecimal.ZERO) != 0) {
                tarefa.setDivergente(true);

                // Aplica Ajuste no Estoque
                // ATENÇÃO: Precisa saber Produto e Lote. Se a tarefa for genérica por local,
                // o operador deveria ter informado isso na contagem (ContagemRequest
                // detalhado).
                // Assumindo aqui que temos o produtoFoco definido na tarefa:
                if (tarefa.getProdutoFoco() != null) {
                    estoqueService.movimentar(
                            tarefa.getProdutoFoco().getId(),
                            tarefa.getLocalizacao().getId(),
                            diferenca.abs(), // Quantidade
                            null, null, null, // LPN/Lote/Serial (Limitação desta simplificação)
                            StatusQualidade.DISPONIVEL,
                            diferenca.compareTo(BigDecimal.ZERO) > 0 ? TipoMovimento.AJUSTE_POSITIVO
                                    : TipoMovimento.AJUSTE_NEGATIVO, // Ou AJUSTE_INVENTARIO
                            usuario,
                            "Inventário " + inventario.getId());
                }
            }
        }

        inventario.setStatus(StatusInventario.FINALIZADO);
        inventarioRepository.save(inventario);
    }
}