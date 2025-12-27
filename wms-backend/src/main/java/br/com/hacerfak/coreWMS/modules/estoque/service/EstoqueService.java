package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.event.EstoqueMovimentadoEvent; // <--- Importante
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LocalizacaoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.MovimentoEstoqueRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher; // <--- Importante
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueSaldoRepository saldoRepository;
    private final MovimentoEstoqueRepository movimentoRepository;
    private final ProdutoRepository produtoRepository;
    private final LocalizacaoRepository localizacaoRepository;
    private final LpnRepository lpnRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Retryable(retryFor = { OptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void movimentar(Long produtoId, Long localId, BigDecimal quantidade,
            String lpn, String lote, String serial,
            StatusQualidade qualidade,
            TipoMovimento tipo, String usuario, String obs) {

        if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));

        Localizacao local = localizacaoRepository.findById(localId)
                .orElseThrow(() -> new EntityNotFoundException("Local não encontrado"));

        validarLocal(local);

        boolean isEntrada = tipo == TipoMovimento.ENTRADA ||
                tipo == TipoMovimento.AJUSTE_POSITIVO ||
                tipo == TipoMovimento.DESBLOQUEIO;

        if (isEntrada && serial != null && !serial.isBlank()) {
            boolean serialJaExiste = saldoRepository.existsByProdutoIdAndNumeroSerie(produtoId, serial);
            if (serialJaExiste) {
                throw new IllegalArgumentException(
                        String.format("O Serial '%s' já consta no estoque para o produto %d.", serial, produtoId));
            }
        }

        EstoqueSaldo saldo = saldoRepository.buscarSaldoExato(
                produtoId, localId, lpn, lote, serial, qualidade).orElse(null);

        BigDecimal saldoAnterior = (saldo != null) ? saldo.getQuantidade() : BigDecimal.ZERO;
        BigDecimal saldoFinal;

        if (isEntrada) {
            if (saldo == null) {
                saldo = EstoqueSaldo.builder()
                        .produto(produto)
                        .localizacao(local)
                        .lpn(lpn)
                        .lote(lote)
                        .numeroSerie(serial)
                        .statusQualidade(qualidade != null ? qualidade : StatusQualidade.DISPONIVEL)
                        .quantidade(BigDecimal.ZERO)
                        .quantidadeReservada(BigDecimal.ZERO)
                        .build();
            }
            saldo.setQuantidade(saldo.getQuantidade().add(quantidade));
        } else {
            if (saldo == null) {
                throw new IllegalArgumentException("Saldo não encontrado para saída.");
            }
            if (saldo.getQuantidade().compareTo(quantidade) < 0) {
                throw new IllegalArgumentException("Saldo físico insuficiente.");
            }
            saldo.setQuantidade(saldo.getQuantidade().subtract(quantidade));
        }

        saldoFinal = saldo.getQuantidade();

        if (saldoFinal.compareTo(BigDecimal.ZERO) == 0
                && saldo.getQuantidadeReservada().compareTo(BigDecimal.ZERO) == 0) {
            saldoRepository.delete(saldo);
        } else {
            saldoRepository.save(saldo);
        }

        gerarMovimento(tipo, produto, local, quantidade, lpn, lote, serial,
                saldoAnterior, saldoFinal, usuario, obs);

        // --- PUBLICAR EVENTO ---
        // Desacopla a lógica: avisa que o estoque mudou.
        // O Listener de Ressuprimento vai pegar isso e decidir se gera tarefa.
        eventPublisher.publishEvent(new EstoqueMovimentadoEvent(
                produtoId,
                localId,
                quantidade,
                saldoFinal,
                tipo.name()));
    }

    private void validarLocal(Localizacao local) {
        if (!local.isAtivo())
            throw new IllegalArgumentException("Local inativo.");
        if (local.isBloqueado())
            throw new IllegalArgumentException("Local bloqueado.");
    }

    private void gerarMovimento(TipoMovimento tipo, Produto produto, Localizacao local,
            BigDecimal qtd, String lpn, String lote, String serial,
            BigDecimal saldoAnt, BigDecimal saldoAtu,
            String usuario, String obs) {
        MovimentoEstoque historico = MovimentoEstoque.builder()
                .tipo(tipo)
                .produto(produto)
                .localizacao(local)
                .quantidade(qtd)
                .saldoAnterior(saldoAnt)
                .saldoAtual(saldoAtu)
                .lpn(lpn)
                .lote(lote)
                .numeroSerie(serial)
                .usuarioResponsavel(usuario)
                .observacao(obs)
                .build();
        movimentoRepository.save(historico);
    }

    @Transactional
    public void transferirLpnInteira(Lpn lpn, Localizacao destino, String usuario, String motivo) {
        if (lpn == null || destino == null)
            return;

        Localizacao origem = lpn.getLocalizacaoAtual();

        // 1. Atualiza a localização física da LPN
        lpn.setLocalizacaoAtual(destino);
        lpnRepository.save(lpn); // Opcional se estiver gerenciado, mas seguro
        // descomentar se der erro

        // 2. Busca os saldos dessa LPN na origem para mover
        // Precisamos encontrar exatamente o que tem nessa LPN na Doca
        // Como o método buscarSaldoExato retorna Optional, precisamos de uma lista se
        // houver múltiplos itens (mix)
        // Se o seu sistema garante 1 produto por LPN, ok. Se for mix, precisaria buscar
        // uma lista.
        // Assumindo estrutura padrão loopando nos itens da LPN é mais seguro:

        for (LpnItem item : lpn.getItens()) {
            // SAÍDA da Origem (Doca)
            movimentar(item.getProduto().getId(), origem.getId(), item.getQuantidade(),
                    lpn.getCodigo(), item.getLote(), item.getNumeroSerie(), item.getStatusQualidade(),
                    TipoMovimento.SAIDA, usuario, motivo + " (Saída)");

            // ENTRADA no Destino (Stage)
            movimentar(item.getProduto().getId(), destino.getId(), item.getQuantidade(),
                    lpn.getCodigo(), item.getLote(), item.getNumeroSerie(), item.getStatusQualidade(),
                    TipoMovimento.ENTRADA, usuario, motivo + " (Entrada)");
        }
    }
}