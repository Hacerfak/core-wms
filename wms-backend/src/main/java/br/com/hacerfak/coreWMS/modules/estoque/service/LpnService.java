package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnItemRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnRepository;
import br.com.hacerfak.coreWMS.modules.operacao.dto.AddItemLpnRequest;
import lombok.RequiredArgsConstructor;
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

    /**
     * 1. PRÉ-GERAÇÃO DE ETIQUETAS
     * Gera N códigos de LPN no banco para o operador imprimir e colar nos pallets
     * vazios.
     */
    @Transactional
    public List<String> gerarLpnsVazias(Integer quantidade, String usuario) {
        List<String> codigosGerados = new ArrayList<>();
        List<Lpn> lpnsParaSalvar = new ArrayList<>(); // Batch para LPN vazia também

        for (int i = 0; i < quantidade; i++) {
            String codigo = gerarCodigoLpnUnico();
            Lpn lpn = Lpn.builder()
                    .codigo(codigo)
                    .tipo(TipoLpn.PALLET)
                    .status(StatusLpn.EM_MONTAGEM)
                    .build();
            lpnsParaSalvar.add(lpn);
            codigosGerados.add(codigo);
        }
        lpnRepository.saveAll(lpnsParaSalvar); // Batch Save
        return codigosGerados;
    }

    /**
     * 2. CONFERÊNCIA (BIPAGEM)
     * Adiciona um produto dentro de uma LPN.
     * Se a LPN não existir (ex: operador está usando uma etiqueta que ele achou
     * perdida
     * ou digitou um código novo manual), o sistema cria na hora.
     */
    @Transactional
    public void adicionarItem(String codigoLpn, AddItemLpnRequest dto, String usuario) {
        // 1. Busca ou Cria a LPN (Container)
        Lpn lpn = lpnRepository.findByCodigo(codigoLpn)
                .orElseGet(() -> {
                    // Se não existe, cria "on the fly"
                    Lpn nova = Lpn.builder()
                            .codigo(codigoLpn)
                            .tipo(TipoLpn.PALLET)
                            .status(StatusLpn.EM_MONTAGEM)
                            .build();
                    return lpnRepository.save(nova);
                });

        validarStatusLpn(lpn);

        // 1. Verifica se já existe no Estoque Físico (Armazenado)
        Produto produto = produtoRepository.findByCodigoBarras(dto.sku())
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));

        // Validação de Serial
        if (dto.numeroSerie() != null && !dto.numeroSerie().isBlank()) {
            if (dto.quantidade().compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalArgumentException("Itens controlados por serial devem ter quantidade igual a 1.");
            }

            // Já fazemos essa busca
            // antes no código
            // original
            if (estoqueSaldoRepository.existsByProdutoIdAndNumeroSerie(produto.getId(), dto.numeroSerie())) {
                throw new IllegalArgumentException("Serial já existente no estoque: " + dto.numeroSerie());
            }

            // 2. Verifica se já foi bipado em OUTRA LPN que ainda está na doca (status
            // EM_MONTAGEM ou FECHADO)
            // Isso evita que dois operadores bipem o mesmo serial em pallets diferentes
            // simultaneamente
            boolean serialEmOutraLpn = lpnItemRepository.existsByProdutoIdAndNumeroSerieAndLpnCodigoNot(
                    produto.getId(), dto.numeroSerie(), codigoLpn);

            if (serialEmOutraLpn) {
                throw new IllegalArgumentException("Serial já conferido em outra LPN: " + dto.numeroSerie());
            }
        }

        // Busca se já existe item igual na LPN para somar (exceto se for serializado,
        // aí cria novo)
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
                    .numeroSerie(dto.numeroSerie()) // <--- Passando o serial
                    .build();
            lpnItemRepository.save(novoItem);
        }
    }

    /**
     * 3. FECHAMENTO DO VOLUME
     * Marca a LPN como pronta para armazenagem (Stage).
     */
    @Transactional
    public void fecharLpn(String codigoLpn, String usuario) {
        Lpn lpn = lpnRepository.findByCodigo(codigoLpn)
                .orElseThrow(() -> new EntityNotFoundException("LPN não encontrada."));

        if (lpn.getItens().isEmpty()) {
            throw new IllegalStateException("Não é possível fechar uma LPN vazia.");
        }

        lpn.setStatus(StatusLpn.FECHADO);
        lpnRepository.save(lpn);
    }

    // --- Auxiliares ---

    private void validarStatusLpn(Lpn lpn) {
        if (lpn.getStatus() != StatusLpn.EM_MONTAGEM) {
            throw new IllegalStateException("LPN " + lpn.getCodigo() + " está " + lpn.getStatus() +
                    " e não pode receber mais itens. (Precisa estar EM_MONTAGEM)");
        }
    }

    private String gerarCodigoLpnUnico() {
        // Formato: LPN + Ano + DiaDoAno + Random (Ex: LPN24300-9999)
        // Simples e curto para código de barras
        String prefix = "LPN-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyDDD"));

        // Tenta gerar até achar um livre (colisão é rara com 4 digitos random, mas
        // segura morreu de velho)
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
     * Cria N LPNs já com o item vinculado e status FECHADO (Pronto para armazenar).
     */
    @Transactional
    public List<String> gerarLpnsComConteudo(
            Produto produto,
            BigDecimal qtdPorVolume,
            Integer qtdVolumes,
            String lote,
            LocalDate validade,
            String numeroSerie,
            Localizacao localizacaoInicial, // <--- NOVO
            Long solicitacaoId,
            String usuario) {

        List<Lpn> lpnsParaSalvar = new ArrayList<>();
        List<EstoqueSaldo> saldosParaSalvar = new ArrayList<>(); // Lista para Batch
        List<String> codigosGerados = new ArrayList<>();

        // Validação de segurança para Serial: Se tiver serial, força 1 volume ou valida
        // unicidade
        if (numeroSerie != null && !numeroSerie.isBlank() && qtdVolumes > 1) {
            // Num cenário real, serial é único. Aqui permitimos, mas fica o alerta de
            // negócio.
            throw new IllegalArgumentException("Itens serializados devem ser gerados um a um.");
        }

        for (int i = 1; i <= qtdVolumes; i++) {
            // Gera código sequencial rápido: LPN-TIMESTAMP-SEQ (Ex: LPN-2310201030-1)
            String codigo = gerarCodigoLpnUnico();
            codigosGerados.add(codigo);

            Lpn lpn = Lpn.builder()
                    .codigo(codigo)
                    .tipo(TipoLpn.PALLET)
                    .status(StatusLpn.FECHADO) // Já nasce fechado, pulando a etapa de montagem
                    .localizacaoAtual(localizacaoInicial)
                    .solicitacaoEntradaId(solicitacaoId)
                    .build();

            // Cria o item dentro da LPN
            LpnItem item = LpnItem.builder()
                    .lpn(lpn) // O JPA/Hibernate gerencia o vínculo se salvo em cascata
                    .produto(produto)
                    .quantidade(qtdPorVolume)
                    .lote(lote)
                    .dataValidade(validade)
                    .numeroSerie(numeroSerie)
                    .build();

            lpn.adicionarItem(item);

            lpnsParaSalvar.add(lpn);

            // Cria objeto de Saldo para salvar em lote depois
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
        }

        // Save All é mais eficiente que salvar um por um
        lpnRepository.saveAll(lpnsParaSalvar);

        estoqueSaldoRepository.saveAll(saldosParaSalvar);

        return codigosGerados;
    }
}