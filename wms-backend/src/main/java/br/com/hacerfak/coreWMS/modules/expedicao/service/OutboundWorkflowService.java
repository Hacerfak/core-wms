package br.com.hacerfak.coreWMS.modules.expedicao.service;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusSolicitacao;
import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.*;
import br.com.hacerfak.coreWMS.modules.expedicao.dto.SolicitacaoSaidaRequest;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.*;
import br.com.hacerfak.coreWMS.modules.expedicao.service.strategy.AlocacaoStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OutboundWorkflowService {

    private final SolicitacaoSaidaRepository solicitacaoRepository;
    private final OndaSeparacaoRepository ondaRepository;
    private final TarefaSeparacaoRepository tarefaRepository;
    private final EstoqueSaldoRepository saldoRepository;
    private final ParceiroRepository parceiroRepository;
    private final ProdutoRepository produtoRepository;
    private final VolumeExpedicaoRepository volumeRepository;

    // Injeta todas as estratégias disponíveis (Map<NomeDoBean, Instancia>)
    private final Map<String, AlocacaoStrategy> estrategias;

    // 1. RECEPÇÃO DE PEDIDO (Integração)
    @Transactional
    public SolicitacaoSaida criarSolicitacao(SolicitacaoSaidaRequest dto) {
        Parceiro cliente = parceiroRepository.findById(dto.clienteId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

        if (solicitacaoRepository.existsByCodigoExterno(dto.codigoExterno())) {
            throw new IllegalArgumentException("Pedido já existe: " + dto.codigoExterno());
        }

        SolicitacaoSaida solicitacao = SolicitacaoSaida.builder()
                .codigoExterno(dto.codigoExterno())
                .cliente(cliente)
                .prioridade(dto.prioridade() != null ? dto.prioridade() : 0)
                .status(StatusSolicitacao.CRIADA)
                .dataLimite(dto.dataLimite())
                .rota(dto.rota())
                .sequenciaEntrega(dto.sequenciaEntrega())
                .build();

        for (var itemDto : dto.itens()) {
            Produto produto = produtoRepository.findById(itemDto.produtoId())
                    .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));

            ItemSolicitacaoSaida item = ItemSolicitacaoSaida.builder()
                    .solicitacao(solicitacao)
                    .produto(produto)
                    .quantidadeSolicitada(itemDto.quantidade())
                    .build();

            solicitacao.getItens().add(item);
        }

        return solicitacaoRepository.save(solicitacao);
    }

    // 2. GERAÇÃO DE ONDA (Planejamento)
    // Agrupa solicitações pendentes em uma nova Onda
    @Transactional
    public OndaSeparacao gerarOndaAutomatica(String rotaAlvo) {
        // Busca solicitações CRIADAS (ainda não processadas)
        // Simplificação: Pega todas. Num cenário real, filtraria por
        // rota/transportadora.
        List<SolicitacaoSaida> pendentes;

        if (rotaAlvo != null && !rotaAlvo.isBlank()) {
            // Cria query no repository: findByStatusAndRota(CRIADA, rotaAlvo)
            pendentes = solicitacaoRepository.findByStatusAndRota(StatusSolicitacao.CRIADA, rotaAlvo);
        } else {
            pendentes = solicitacaoRepository.findByStatus(StatusSolicitacao.CRIADA);
        }

        OndaSeparacao onda = OndaSeparacao.builder()
                .codigo("ONDA-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")))
                .status(StatusOnda.CRIADA)
                .solicitacoes(new ArrayList<>())
                .build();

        onda = ondaRepository.save(onda);

        for (SolicitacaoSaida sol : pendentes) {
            sol.setOnda(onda);
            sol.setStatus(StatusSolicitacao.EM_PROCESSAMENTO);
            solicitacaoRepository.save(sol);
        }

        return onda;
    }

    // 3. ALOCAÇÃO E GERAÇÃO DE TAREFAS
    // Se der erro de concorrência (alguém mexeu no saldo milissegundos antes),
    // o sistema espera 500ms e tenta de novo, até 3 vezes.
    @Transactional
    @Retryable(retryFor = { OptimisticLockException.class }, maxAttempts = 3, backoff = @Backoff(delay = 500))
    public void processarOnda(Long ondaId) {
        OndaSeparacao onda = ondaRepository.findById(ondaId)
                .orElseThrow(() -> new EntityNotFoundException("Onda não encontrada"));

        if (onda.getStatus() != StatusOnda.CRIADA) {
            throw new IllegalStateException("Onda já processada.");
        }

        // Define estratégia (pode vir de config do sistema ou do cliente)
        AlocacaoStrategy strategy = estrategias.get("FEFO");

        for (SolicitacaoSaida solicitacao : onda.getSolicitacoes()) {
            for (ItemSolicitacaoSaida item : solicitacao.getItens()) {
                BigDecimal qtdFaltante = item.getQuantidadeSolicitada().subtract(item.getQuantidadeAlocada());

                if (qtdFaltante.compareTo(BigDecimal.ZERO) <= 0)
                    continue;

                // Usa a estratégia para achar estoques
                List<EstoqueSaldo> saldos = strategy.buscarSaldosCandidatos(item.getProduto(), qtdFaltante);

                for (EstoqueSaldo saldo : saldos) {
                    if (qtdFaltante.compareTo(BigDecimal.ZERO) <= 0)
                        break;

                    BigDecimal disponivel = saldo.getQuantidade().subtract(saldo.getQuantidadeReservada());
                    if (disponivel.compareTo(BigDecimal.ZERO) <= 0)
                        continue;

                    BigDecimal aReservar = disponivel.min(qtdFaltante);

                    // A) Reserva no Saldo
                    saldo.setQuantidadeReservada(saldo.getQuantidadeReservada().add(aReservar));
                    saldoRepository.save(saldo);

                    // B) Cria Tarefa de Picking para esta alocação específica
                    TarefaSeparacao tarefa = TarefaSeparacao.builder()
                            .onda(onda)
                            .produto(item.getProduto())
                            .origem(saldo.getLocalizacao()) // Onde o operador deve ir
                            .loteSolicitado(saldo.getLote()) // O que ele deve pegar
                            .quantidadePlanejada(aReservar)
                            .build();

                    tarefa.setStatus(StatusTarefa.PENDENTE);
                    tarefaRepository.save(tarefa);

                    // C) Atualiza Item
                    item.setQuantidadeAlocada(item.getQuantidadeAlocada().add(aReservar));
                    qtdFaltante = qtdFaltante.subtract(aReservar);
                }
            }
        }

        onda.setStatus(StatusOnda.ALOCADA); // Ou EM_SEPARACAO se liberar direto
        onda.setDataLiberacao(LocalDateTime.now());
        ondaRepository.save(onda);
    }

    // --- MELHORIA 3: LÓGICA DE DESPACHO ---
    @Transactional
    public void realizarConferenciaExpedicao(String codigoRastreio, String usuario) {
        VolumeExpedicao volume = volumeRepository.findByCodigoRastreio(codigoRastreio)
                .orElseThrow(() -> new EntityNotFoundException("Volume não encontrado: " + codigoRastreio));

        if (!volume.isFechado()) {
            throw new IllegalStateException("Volume ainda não foi fechado no Packing.");
        }

        // Se já tiver uma flag de despachado (sugerido criar no futuro), valida aqui.
        // Como não podemos alterar a entidade Volume agora, vamos usar a
        // SolicitacaoSaida
        // para verificar se todos os volumes dela já foram bipados.

        SolicitacaoSaida pedido = volume.getSolicitacao();

        // Lógica de Negócio: O volume foi carregado.
        // Em um sistema real, mudaríamos o status do Volume para 'DESPACHADO'.
        // Aqui, vamos logar e verificar se o PEDIDO inteiro foi concluído.

        System.out.println(">>> CHECK-OUT: Volume " + codigoRastreio + " carregado por " + usuario);

        // Verifica se todos os itens do pedido já foram embalados e se não há volumes
        // abertos
        boolean temVolumesAbertos = volumeRepository.existsBySolicitacaoIdAndFechadoFalse(pedido.getId());

        if (!temVolumesAbertos) {
            // Se tudo está embalado e este volume está saindo, podemos considerar o pedido
            // como CONCLUIDO ou EM_TRANSITO.
            if (pedido.getStatus() != StatusSolicitacao.CONCLUIDA) {
                pedido.setStatus(StatusSolicitacao.CONCLUIDA);
                solicitacaoRepository.save(pedido);
                System.out
                        .println(">>> PEDIDO " + pedido.getCodigoExterno() + " FINALIZADO (Todos volumes despachados)");
            }
        }
    }
}