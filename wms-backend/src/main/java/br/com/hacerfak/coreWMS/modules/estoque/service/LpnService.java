package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.event.LpnCriadaEvent;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.FormatoLpnRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnItemRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.MovimentoEstoqueRepository;
import br.com.hacerfak.coreWMS.modules.operacao.domain.SolicitacaoEntrada;
import br.com.hacerfak.coreWMS.modules.operacao.dto.AddItemLpnRequest;
import br.com.hacerfak.coreWMS.modules.operacao.repository.ItemSolicitacaoEntradaRepository;
import br.com.hacerfak.coreWMS.modules.operacao.repository.SolicitacaoEntradaRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class LpnService {

    private final LpnRepository lpnRepository;
    private final LpnItemRepository lpnItemRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueSaldoRepository estoqueSaldoRepository;
    private final FormatoLpnRepository formatoLpnRepository;
    private final MovimentoEstoqueRepository movimentoEstoqueRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ItemSolicitacaoEntradaRepository itemSolicitacaoRepository;
    private final SolicitacaoEntradaRepository solicitacaoEntradaRepository;

    /**
     * 1. PRÉ-GERAÇÃO DE ETIQUETAS
     * Gera N códigos de LPN no banco para o operador imprimir e colar nos pallets
     * vazios.
     * ALTERAÇÃO: Recebe formatoId para vincular ao tipo físico (PBR, Caixa, etc).
     */
    @Transactional
    public List<String> gerarLpnsVazias(Integer quantidade, Long formatoId, Long solicitacaoId,
            String usuario) {
        // 1. Busca o formato escolhido
        FormatoLpn formato = formatoLpnRepository.findById(formatoId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Formato de LPN não encontrado (ID: " + formatoId + ")"));

        // 2. Busca a Solicitação de Entrada para pegar a Doca vinculada
        // (Assumindo que sua classe se chama SolicitacaoEntrada)
        SolicitacaoEntrada solicitacao = solicitacaoEntradaRepository.findById(solicitacaoId)
                .orElseThrow(
                        () -> new EntityNotFoundException("Solicitação não encontrada (ID: " + solicitacaoId + ")"));

        // 3. Obtém a doca a partir da solicitação encontrada
        // Verifique se no seu modelo o método é getDoca() ou getLocalizacao()
        Localizacao doca = solicitacao.getDoca();

        // Validação extra opcional: Se a solicitação não tiver doca, impede a criação
        if (doca == null) {
            throw new IllegalStateException("A solicitação (ID: " + solicitacaoId + ") não possui uma doca vinculada.");
        }

        List<String> codigosGerados = new ArrayList<>();
        List<Lpn> lpnsParaSalvar = new ArrayList<>();

        for (int i = 0; i < quantidade; i++) {
            String codigo = gerarCodigoLpnUnico();

            Lpn lpn = Lpn.builder()
                    .codigo(codigo)
                    .formato(formato)
                    .status(StatusLpn.EM_MONTAGEM)
                    .solicitacaoEntradaId(solicitacaoId)
                    .localizacaoAtual(doca) // Usa a doca recuperada da solicitação
                    .criadoPor(usuario)
                    .build();

            lpnsParaSalvar.add(lpn);
            codigosGerados.add(codigo);
        }

        lpnRepository.saveAll(lpnsParaSalvar);
        return codigosGerados;
    }

    /**
     * 2. CONFERÊNCIA (BIPAGEM)
     * Adiciona um produto dentro de uma LPN.
     * ALTERAÇÃO: Recebe formatoId (Opcional) caso precise criar a LPN "on the fly".
     */
    @Transactional
    public void adicionarItem(String codigoLpn, AddItemLpnRequest dto, Long formatoId, String usuario) {
        // 1. Busca ou Cria a LPN (Container)
        Lpn lpn = lpnRepository.findByCodigo(codigoLpn)
                .orElseGet(() -> {
                    // Se não existe, cria "on the fly".
                    // Para criar, PRECISAMOS do formatoId informado pelo operador na tela.
                    if (formatoId == null) {
                        throw new IllegalArgumentException(
                                "LPN nova detectada. Informe o Formato (Pallet/Caixa) para prosseguir.");
                    }

                    FormatoLpn formato = formatoLpnRepository.findById(formatoId)
                            .orElseThrow(() -> new EntityNotFoundException("Formato inválido."));

                    Lpn nova = Lpn.builder()
                            .codigo(codigoLpn)
                            .formato(formato) // Vincula o formato escolhido na tela
                            .status(StatusLpn.EM_MONTAGEM)
                            .criadoPor(usuario)
                            .build();
                    return lpnRepository.save(nova);
                });

        validarStatusLpn(lpn);

        // --- LÓGICA ORIGINAL DE VALIDAÇÃO DE ITEM/SERIAL MANTIDA ABAIXO ---

        Produto produto = produtoRepository.findByCodigoBarras(dto.sku())
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));

        // Validação de Serial
        if (dto.numeroSerie() != null && !dto.numeroSerie().isBlank()) {
            if (dto.quantidade().compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("Itens controlados por serial devem ter quantidade igual a 1.");
            }

            if (estoqueSaldoRepository.existsByProdutoIdAndNumeroSerie(produto.getId(), dto.numeroSerie())) {
                throw new IllegalArgumentException("Serial já existente no estoque: " + dto.numeroSerie());
            }

            boolean serialEmOutraLpn = lpnItemRepository.existsByProdutoIdAndNumeroSerieAndLpnCodigoNot(
                    produto.getId(), dto.numeroSerie(), codigoLpn);

            if (serialEmOutraLpn) {
                throw new IllegalArgumentException("Serial já conferido em outra LPN: " + dto.numeroSerie());
            }
        }

        // Upsert do Item
        Optional<LpnItem> itemExistente = Optional.empty();

        if (dto.numeroSerie() == null) {
            itemExistente = lpnItemRepository.findByLpnIdAndProdutoIdAndLote(
                    lpn.getId(), produto.getId(), dto.lote());
        }

        if (itemExistente.isPresent()) {
            LpnItem item = itemExistente.get();
            item.setQuantidade(item.getQuantidade().add(dto.quantidade()));
            lpnItemRepository.save(item);
        } else {
            LpnItem novoItem = LpnItem.builder()
                    .lpn(lpn)
                    .produto(produto)
                    .quantidade(dto.quantidade())
                    .lote(dto.lote())
                    .dataValidade(dto.dataValidade())
                    .statusQualidade(dto.statusQualidade() != null ? dto.statusQualidade() : StatusQualidade.DISPONIVEL)
                    .numeroSerie(dto.numeroSerie())
                    .build();
            lpnItemRepository.save(novoItem);
        }
    }

    /**
     * 3. FECHAMENTO DO VOLUME (Sem alterações, apenas mantido)
     */
    @Transactional
    public void fecharLpn(String codigoLpn, String usuario) {
        Lpn lpn = lpnRepository.findByCodigo(codigoLpn)
                .orElseThrow(() -> new EntityNotFoundException("LPN não encontrada."));

        if (lpn.getStatus() != StatusLpn.EM_MONTAGEM) {
            // Se já estiver fechado, apenas retorna (idempotente) ou erro
            if (lpn.getStatus() == StatusLpn.FECHADO)
                return;
            throw new IllegalStateException("LPN não está em montagem.");
        }

        if (lpn.getItens().isEmpty()) {
            throw new IllegalStateException("Não é possível fechar uma LPN vazia.");
        }

        // Valida se tem localização (Doca) vinculada. Se não tiver, tenta achar a da
        // solicitação.
        if (lpn.getLocalizacaoAtual() == null && lpn.getSolicitacaoEntradaId() != null) {
            // Lógica para recuperar a doca da solicitação (opcional, ou falha)
            // Idealmente a LPN já nasce na doca.
        }

        Localizacao local = lpn.getLocalizacaoAtual();
        if (local == null)
            throw new IllegalStateException("LPN sem localização definida (Doca).");

        // PROCESSA CADA ITEM DO VOLUME
        for (LpnItem item : lpn.getItens()) {
            // 1. Cria Estoque Físico
            EstoqueSaldo saldo = EstoqueSaldo.builder()
                    .produto(item.getProduto())
                    .localizacao(local)
                    .lpn(lpn.getCodigo())
                    .lote(item.getLote())
                    .dataValidade(item.getDataValidade())
                    .numeroSerie(item.getNumeroSerie())
                    .statusQualidade(item.getStatusQualidade())
                    .quantidade(item.getQuantidade())
                    .quantidadeReservada(BigDecimal.ZERO)
                    .build();
            estoqueSaldoRepository.save(saldo);

            // 2. Gera Kardex (Entrada)
            MovimentoEstoque mov = MovimentoEstoque.builder()
                    .tipo(TipoMovimento.ENTRADA)
                    .produto(item.getProduto())
                    .localizacao(local)
                    .quantidade(item.getQuantidade())
                    .saldoAnterior(BigDecimal.ZERO)
                    .saldoAtual(item.getQuantidade())
                    .lpn(lpn.getCodigo())
                    .lote(item.getLote())
                    .numeroSerie(item.getNumeroSerie())
                    .usuarioResponsavel(usuario)
                    .observacao("Fechamento Volume Misto (Sol. " + lpn.getSolicitacaoEntradaId() + ")")
                    .build();
            movimentoEstoqueRepository.save(mov);

            // 3. Atualiza Progresso da Solicitação
            if (lpn.getSolicitacaoEntradaId() != null) {
                itemSolicitacaoRepository.somarQuantidadeConferida(
                        lpn.getSolicitacaoEntradaId(),
                        item.getProduto().getId(),
                        item.getQuantidade());
            }
        }

        lpn.setStatus(StatusLpn.FECHADO);
        lpnRepository.save(lpn);

        // Opcional:
        String currentTenant = TenantContext.getTenant();
        eventPublisher.publishEvent(new LpnCriadaEvent(lpn.getId(), lpn.getCodigo(), currentTenant));

    }

    // --- Auxiliares ---

    private void validarStatusLpn(Lpn lpn) {
        if (lpn.getStatus() != StatusLpn.EM_MONTAGEM) {
            throw new IllegalStateException("LPN " + lpn.getCodigo() + " está " + lpn.getStatus() +
                    " e não pode receber mais itens. (Precisa estar EM_MONTAGEM)");
        }
    }

    private String gerarCodigoLpnUnico() {
        String prefix = "LPN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyDDD"));
        for (int i = 0; i < 5; i++) {
            int random = ThreadLocalRandom.current().nextInt(1000, 9999);
            String codigo = prefix + "-" + random;
            if (!lpnRepository.existsByCodigo(codigo)) {
                return codigo;
            }
        }
        throw new RuntimeException("Falha ao gerar código LPN único. Tente novamente.");
    }

    /**
     * GERAÇÃO EM MASSA (Carga Fechada / Monoproduto)
     * ALTERAÇÃO: Adicionado parâmetro Long formatoId.
     */
    @Transactional
    public List<String> gerarLpnsComConteudo(
            Produto produto,
            BigDecimal qtdPorVolume,
            Integer qtdVolumes,
            String lote,
            LocalDate validade,
            String numeroSerie,
            Localizacao localizacaoInicial,
            Long solicitacaoId,
            Long formatoId,
            String usuario) {

        // Busca o formato uma única vez para usar em todas as LPNs do loop
        FormatoLpn formato = formatoLpnRepository.findById(formatoId)
                .orElseThrow(() -> new EntityNotFoundException("Formato de LPN não encontrado."));

        List<Lpn> lpnsParaSalvar = new ArrayList<>();
        List<EstoqueSaldo> saldosParaSalvar = new ArrayList<>();
        List<MovimentoEstoque> movimentosParaSalvar = new ArrayList<>();
        List<String> codigosGerados = new ArrayList<>();

        if (numeroSerie != null && !numeroSerie.isBlank() && qtdVolumes > 1) {
            throw new IllegalArgumentException("Itens serializados devem ser gerados um a um.");
        }

        String currentTenant = TenantContext.getTenant();

        for (int i = 1; i <= qtdVolumes; i++) {
            String codigo = gerarCodigoLpnUnico();
            codigosGerados.add(codigo);

            Lpn lpn = Lpn.builder()
                    .codigo(codigo)
                    .formato(formato) // Vínculo com a entidade FormatoLpn
                    .status(StatusLpn.FECHADO)
                    .localizacaoAtual(localizacaoInicial)
                    .solicitacaoEntradaId(solicitacaoId)
                    .criadoPor(usuario) // Boa prática registrar quem gerou a massa
                    .build();

            LpnItem item = LpnItem.builder()
                    .lpn(lpn)
                    .produto(produto)
                    .quantidade(qtdPorVolume)
                    .lote(lote)
                    .dataValidade(validade)
                    .numeroSerie(numeroSerie)
                    .statusQualidade(StatusQualidade.DISPONIVEL) // Default importante
                    .build();

            lpn.setItens(new ArrayList<>()); // Garante lista inicializada caso precise
            lpn.getItens().add(item); // Vínculo bidirecional se necessário pelo Cascade

            lpnsParaSalvar.add(lpn);

            saldosParaSalvar.add(EstoqueSaldo.builder()
                    .produto(produto)
                    .localizacao(localizacaoInicial)
                    .lpn(codigo)
                    .lote(lote)
                    .numeroSerie(numeroSerie)
                    .statusQualidade(StatusQualidade.DISPONIVEL)
                    .quantidade(qtdPorVolume)
                    .quantidadeReservada(BigDecimal.ZERO)
                    .build());

            // 4. --- CORREÇÃO: Cria o Registro de Movimentação (Kardex) ---
            // Como é um registro novo (saldo novo), o anterior é zero.
            MovimentoEstoque movimento = MovimentoEstoque.builder()
                    .tipo(TipoMovimento.ENTRADA) // Marca como Entrada
                    .produto(produto)
                    .localizacao(localizacaoInicial)
                    .quantidade(qtdPorVolume)
                    .saldoAnterior(BigDecimal.ZERO)
                    .saldoAtual(qtdPorVolume)
                    .lpn(codigo)
                    .lote(lote)
                    .numeroSerie(numeroSerie)
                    .usuarioResponsavel(usuario)
                    .observacao("Recebimento - Geração de LPN (Solicitação " + solicitacaoId + ")")
                    .build();

            movimentosParaSalvar.add(movimento);
        }

        List<Lpn> lpnsSalvas = lpnRepository.saveAll(lpnsParaSalvar);
        estoqueSaldoRepository.saveAll(saldosParaSalvar);
        movimentoEstoqueRepository.saveAll(movimentosParaSalvar);

        for (Lpn lpn : lpnsSalvas) {
            eventPublisher.publishEvent(new LpnCriadaEvent(
                    lpn.getId(),
                    lpn.getCodigo(),
                    currentTenant));
        }

        return codigosGerados;
    }
}