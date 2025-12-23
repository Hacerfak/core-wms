package br.com.hacerfak.coreWMS.modules.expedicao.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Lpn;
import br.com.hacerfak.coreWMS.modules.estoque.repository.LpnRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.*;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.ItemVolumeExpedicaoRepository; // Injetar
import br.com.hacerfak.coreWMS.modules.expedicao.repository.SolicitacaoSaidaRepository;
import br.com.hacerfak.coreWMS.modules.expedicao.repository.VolumeExpedicaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PackingService {

    private final SolicitacaoSaidaRepository solicitacaoRepository;
    private final VolumeExpedicaoRepository volumeRepository;
    private final ProdutoRepository produtoRepository;
    private final LpnRepository lpnRepository;
    private final ItemVolumeExpedicaoRepository itemVolumeRepository; // <--- Novo repositório injetado

    @Transactional
    public VolumeExpedicao abrirNovoVolume(Long solicitacaoId, String tipoEmbalagem) {
        SolicitacaoSaida solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado"));

        VolumeExpedicao volume = VolumeExpedicao.builder()
                .solicitacao(solicitacao)
                .codigoRastreio("VOL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .tipoEmbalagem(tipoEmbalagem)
                .fechado(false)
                .build();

        return volumeRepository.save(volume);
    }

    @Transactional
    public void conferirItemOuLpn(Long volumeId, String codigoBipado) {
        VolumeExpedicao volume = volumeRepository.findById(volumeId)
                .orElseThrow(() -> new EntityNotFoundException("Volume não encontrado"));

        if (volume.isFechado()) {
            throw new IllegalStateException("Volume já está fechado.");
        }

        Optional<Lpn> lpnOpt = lpnRepository.findByCodigo(codigoBipado);

        if (lpnOpt.isPresent()) {
            // Conferência por LPN (Pallet Fechado)
            Lpn lpn = lpnOpt.get();
            lpn.getItens().forEach(itemLpn -> {
                adicionarItemNoVolume(volume, itemLpn.getProduto(), itemLpn.getQuantidade());
            });
        } else {
            // Conferência Unitária (Item)
            Produto produto = produtoRepository.findByCodigoBarras(codigoBipado)
                    .orElseThrow(() -> new EntityNotFoundException("Código não identificado: " + codigoBipado));

            adicionarItemNoVolume(volume, produto, BigDecimal.ONE);
        }
    }

    private void adicionarItemNoVolume(VolumeExpedicao volume, Produto produto, BigDecimal qtdParaAdicionar) {
        // --- BLIND CHECK (Validação de Quantidade) ---

        // 1. Busca quanto foi solicitado no Pedido Original
        ItemSolicitacaoSaida itemSolicitado = volume.getSolicitacao().getItens().stream()
                .filter(i -> i.getProduto().getId().equals(produto.getId()))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Produto não pertence a este pedido: " + produto.getSku()));

        // 2. Busca quanto JÁ foi conferido em TODOS os volumes deste pedido
        BigDecimal totalJaConferido = itemVolumeRepository.somarTotalEmbaladoPorProduto(
                volume.getSolicitacao().getId(),
                produto.getId());

        // 3. Verifica se vai estourar
        BigDecimal totalAposAdicao = totalJaConferido.add(qtdParaAdicionar);

        if (totalAposAdicao.compareTo(itemSolicitado.getQuantidadeSolicitada()) > 0) {
            throw new IllegalStateException(String.format(
                    "Excesso de quantidade! Solicitado: %s, Já Conferido: %s, Tentando Adicionar: %s",
                    itemSolicitado.getQuantidadeSolicitada(), totalJaConferido, qtdParaAdicionar));
        }
        // ---------------------------------------------

        // Se passou no check, adiciona no volume atual
        Optional<ItemVolumeExpedicao> itemExistenteNoVolume = volume.getItens().stream()
                .filter(i -> i.getProduto().getId().equals(produto.getId()))
                .findFirst();

        if (itemExistenteNoVolume.isPresent()) {
            ItemVolumeExpedicao item = itemExistenteNoVolume.get();
            item.setQuantidade(item.getQuantidade().add(qtdParaAdicionar));
            // O save do volume (Cascade) ou do item resolve, mas salvar o item
            // explicitamente é seguro
            // itemVolumeRepository.save(item); // Opcional se Cascade estiver OK
        } else {
            ItemVolumeExpedicao novoItem = ItemVolumeExpedicao.builder()
                    .volume(volume)
                    .produto(produto)
                    .quantidade(qtdParaAdicionar)
                    .build();
            volume.getItens().add(novoItem);
        }
        volumeRepository.save(volume);
    }

    @Transactional
    public VolumeExpedicao fecharVolume(Long volumeId, BigDecimal peso) {
        VolumeExpedicao volume = volumeRepository.findById(volumeId)
                .orElseThrow(() -> new EntityNotFoundException("Volume não encontrado"));

        volume.setPesoBruto(peso);
        volume.setFechado(true);
        return volumeRepository.save(volume);
    }
}