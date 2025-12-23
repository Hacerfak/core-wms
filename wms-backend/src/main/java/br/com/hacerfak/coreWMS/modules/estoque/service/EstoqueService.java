package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LocalizacaoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.MovimentoEstoqueRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final EstoqueSaldoRepository saldoRepository;
    private final MovimentoEstoqueRepository movimentoRepository;
    private final ProdutoRepository produtoRepository;
    private final LocalizacaoRepository localizacaoRepository;

    @Transactional
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

        // --- NOVA VALIDAÇÃO DE SERIAL ---
        if (isEntrada && serial != null && !serial.isBlank()) {
            // Regra: Serial deve ser único para o produto no estoque inteiro
            boolean serialJaExiste = saldoRepository.existsByProdutoIdAndNumeroSerie(produtoId, serial);

            if (serialJaExiste) {
                // Caso extremo: Se estamos fazendo um estorno ou ajuste, o serial pode já
                // existir no banco mas zerado.
                // A query do repository já filtra por quantidade > 0, então se retornou true, é
                // duplicidade real.
                throw new IllegalArgumentException(
                        String.format("O Serial '%s' já consta no estoque para o produto %d.", serial, produtoId));
            }
        }

        // Busca o saldo ou retorna null
        EstoqueSaldo saldo = saldoRepository.buscarSaldoExato(
                produtoId, localId, lpn, lote, serial, qualidade).orElse(null);

        // --- Captura Snapshot Antes ---
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
            // Saída
            if (saldo == null) {
                throw new IllegalArgumentException("Saldo não encontrado para saída.");
            }
            if (saldo.getQuantidade().compareTo(quantidade) < 0) {
                throw new IllegalArgumentException("Saldo físico insuficiente.");
            }
            saldo.setQuantidade(saldo.getQuantidade().subtract(quantidade));
        }

        // --- Captura Snapshot Depois ---
        saldoFinal = saldo.getQuantidade();

        // Persiste Saldo
        if (saldoFinal.compareTo(BigDecimal.ZERO) == 0
                && saldo.getQuantidadeReservada().compareTo(BigDecimal.ZERO) == 0) {
            saldoRepository.delete(saldo);
        } else {
            saldoRepository.save(saldo);
        }

        // Persiste Histórico (Kardex)
        gerarMovimento(tipo, produto, local, quantidade, lpn, lote, serial,
                saldoAnterior, saldoFinal, usuario, obs);
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
                .saldoAnterior(saldoAnt) // Gravado
                .saldoAtual(saldoAtu) // Gravado
                .lpn(lpn)
                .lote(lote)
                .numeroSerie(serial)
                .usuarioResponsavel(usuario)
                .observacao(obs)
                .build();
        movimentoRepository.save(historico);
    }
}