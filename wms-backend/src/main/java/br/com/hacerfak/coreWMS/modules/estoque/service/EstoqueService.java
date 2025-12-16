package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LocalizacaoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.repository.MovimentoEstoqueRepository;
import br.com.hacerfak.coreWMS.modules.operacao.domain.VolumeRecebimento;
import br.com.hacerfak.coreWMS.modules.operacao.repository.VolumeRecebimentoRepository;
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
    private final VolumeRecebimentoRepository volumeRepository;

    /**
     * 1. ARMAZENAGEM DE LPN (Novo Fluxo de Recebimento)
     * Transforma um Volume Virtual (VolumeRecebimento) em Saldo Real
     * (EstoqueSaldo).
     */
    @Transactional
    public void armazenarLpn(String lpn, Long localDestinoId, String usuario) {
        // Valida Volume
        VolumeRecebimento volume = volumeRepository.findByLpn(lpn)
                .orElseThrow(() -> new EntityNotFoundException("Volume/LPN não encontrado: " + lpn));

        if (volume.isArmazenado()) {
            throw new IllegalArgumentException("Este LPN já foi armazenado anteriormente!");
        }

        // Valida Local
        Localizacao local = localizacaoRepository.findById(localDestinoId)
                .orElseThrow(() -> new EntityNotFoundException("Local de destino não encontrado"));

        validarLocal(local);

        // Cria o Saldo Físico com LPN
        EstoqueSaldo novoSaldo = EstoqueSaldo.builder()
                .produto(volume.getProduto())
                .localizacao(local)
                .lpn(volume.getLpn()) // VITAL: O saldo nasce amarrado a este ID
                .quantidade(volume.getQuantidadeOriginal())
                .quantidadeReservada(BigDecimal.ZERO)
                .lote(null)
                .numeroSerie(null)
                .build();

        saldoRepository.save(novoSaldo);

        // Gera Histórico
        gerarMovimento(TipoMovimento.ENTRADA, volume.getProduto(), local,
                volume.getQuantidadeOriginal(), volume.getLpn(), null, null,
                usuario, "Armazenagem Recebimento " + volume.getRecebimento().getId());

        // Atualiza o Volume (Baixa a pendência)
        volume.setArmazenado(true);
        volume.setLocalDestino(local);
        volumeRepository.save(volume);
    }

    /**
     * 2. MOVIMENTAÇÃO GENÉRICA (Ajustes, Picking, Transferências)
     * Agora suporta LPN (se informado) ou Produto Solto (se lpn for null).
     */
    @Transactional
    public void movimentar(Long produtoId, Long localId, BigDecimal quantidade,
            String lpn, String lote, String serial,
            TipoMovimento tipo, String usuario, String obs) {

        if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));

        Localizacao local = localizacaoRepository.findById(localId)
                .orElseThrow(() -> new EntityNotFoundException("Local não encontrado"));

        validarLocal(local);

        // Define sinal matemático (Entrada soma, Saída subtrai)
        boolean isEntrada = tipo == TipoMovimento.ENTRADA ||
                tipo == TipoMovimento.AJUSTE_POSITIVO ||
                tipo == TipoMovimento.DESBLOQUEIO;

        // Busca saldo exato (considerando se tem LPN ou não)
        EstoqueSaldo saldo = saldoRepository.buscarSaldoExato(produtoId, localId, lpn, lote, serial)
                .orElse(null);

        if (isEntrada) {
            if (saldo == null) {
                saldo = EstoqueSaldo.builder()
                        .produto(produto)
                        .localizacao(local)
                        .lpn(lpn)
                        .lote(lote)
                        .numeroSerie(serial)
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

            // --- CORREÇÃO DO AVISO "VARIABLE NOT USED" ---
            // Valida saldo disponível (Físico - Reservado)
            BigDecimal disponivel = saldo.getQuantidade().subtract(saldo.getQuantidadeReservada());

            // Usamos a variável 'disponivel' para validar, garantindo que não consumimos
            // estoque reservado
            if (disponivel.compareTo(quantidade) < 0) {
                throw new IllegalArgumentException("Saldo disponível insuficiente. Físico: " + saldo.getQuantidade()
                        + ", Reservado: " + saldo.getQuantidadeReservada() + ", Solicitado: " + quantidade);
            }

            saldo.setQuantidade(saldo.getQuantidade().subtract(quantidade));

            // Se zerou e tem LPN, podemos querer deletar o registro para não ficar lixo no
            // banco
            // Mas cuidado com logs. Por enquanto, mantemos zerado.
        }

        saldoRepository.save(saldo);

        gerarMovimento(tipo, produto, local, quantidade, lpn, lote, serial, usuario, obs);
    }

    // --- Métodos Privados Auxiliares ---

    private void validarLocal(Localizacao local) {
        if (!local.isAtivo()) {
            throw new IllegalArgumentException("Local inativo: " + local.getCodigo());
        }
        if (local.isBloqueado()) {
            // Permitimos saída de local bloqueado? Geralmente sim (para esvaziar).
            // Mas entrada não. Fica a critério da regra.
            // Vamos bloquear tudo por segurança padrão.
            throw new IllegalArgumentException("Local bloqueado: " + local.getCodigo());
        }
    }

    private void gerarMovimento(TipoMovimento tipo, Produto produto, Localizacao local,
            BigDecimal qtd, String lpn, String lote, String serial,
            String usuario, String obs) {
        MovimentoEstoque historico = MovimentoEstoque.builder()
                .tipo(tipo)
                .produto(produto)
                .localizacao(local)
                .quantidade(qtd)
                .lpn(lpn)
                .lote(lote)
                .numeroSerie(serial)
                .usuarioResponsavel(usuario)
                .observacao(obs)
                .build();
        movimentoRepository.save(historico);
    }
}