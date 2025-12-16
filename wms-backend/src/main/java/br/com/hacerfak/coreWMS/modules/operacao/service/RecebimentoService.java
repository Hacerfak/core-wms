package br.com.hacerfak.coreWMS.modules.operacao.service;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ProdutoRepository;
import br.com.hacerfak.coreWMS.modules.operacao.domain.*;
import br.com.hacerfak.coreWMS.modules.operacao.dto.ConferenciaRequest;
import br.com.hacerfak.coreWMS.modules.operacao.repository.RecebimentoRepository;
import br.com.hacerfak.coreWMS.modules.operacao.repository.VolumeRecebimentoRepository;
import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException; // Sua exception customizada

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecebimentoService {

    private final RecebimentoRepository recebimentoRepository;
    private final VolumeRecebimentoRepository volumeRepository;
    private final ProdutoRepository produtoRepository;

    /**
     * PASSO 1: Operador conta, sistema gera o Volume (LPN).
     * Retorna o código LPN para o front mandar imprimir.
     */
    @Transactional
    public String gerarVolume(Long recebimentoId, ConferenciaRequest dto, String usuarioResponsavel) {
        Recebimento recebimento = recebimentoRepository.findById(recebimentoId)
                .orElseThrow(() -> new EntityNotFoundException("Recebimento não encontrado"));

        if (recebimento.getStatus() == StatusRecebimento.FINALIZADO) {
            throw new IllegalStateException("Recebimento já finalizado.");
        }

        // --- MUDANÇA AQUI: Busca Flexível (SKU, EAN ou DUN) ---
        // O campo 'dto.sku()' agora pode conter qualquer código
        Produto produto = produtoRepository.findByCodigoBarras(dto.sku())
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado com o código: " + dto.sku()));

        // Validação extra: O produto bipado pertence a essa nota?
        boolean pertenceNota = recebimento.getItens().stream()
                .anyMatch(item -> item.getProduto().getId().equals(produto.getId()));

        if (!pertenceNota) {
            throw new IllegalArgumentException("Produto " + produto.getNome() + " não pertence a esta Nota Fiscal!");
        }

        // 2. Gera o LPN (License Plate Number)
        // Se o operador não bipou um código externo, geramos um nosso.
        String lpn = (dto.lpnExterno() != null && !dto.lpnExterno().isBlank())
                ? dto.lpnExterno()
                : gerarLpnUnico(recebimentoId, produto);

        if (volumeRepository.existsByLpn(lpn)) {
            throw new IllegalArgumentException("LPN já existe no sistema: " + lpn);
        }

        // 3. Cria o Volume
        VolumeRecebimento volume = VolumeRecebimento.builder()
                .recebimento(recebimento)
                .produto(produto)
                .lpn(lpn)
                .quantidadeOriginal(dto.quantidade())
                .armazenado(false)
                .dataCriacao(LocalDateTime.now())
                .usuarioCriacao(usuarioResponsavel) // <--- AGORA É REAL! Antes era "OPERADOR"
                .build();

        volumeRepository.save(volume);

        // Atualiza quantidade conferida no item
        ItemRecebimento item = recebimento.getItens().stream()
                .filter(i -> i.getProduto().getId().equals(produto.getId()))
                .findFirst()
                .orElseThrow();

        // Somamos a quantidade
        item.setQuantidadeConferida(item.getQuantidadeConferida().add(dto.quantidade()));

        // 4. Atualiza status se for o primeiro bip
        if (recebimento.getStatus() == StatusRecebimento.AGUARDANDO) {
            recebimento.setStatus(StatusRecebimento.EM_CONFERENCIA);
            recebimentoRepository.save(recebimento);
        }

        // --- CORREÇÃO: SALVAR EXPLICITAMENTE O RECEBIMENTO ---
        // Isso força a atualização do ItemRecebimento no banco
        recebimentoRepository.save(recebimento);

        return lpn;
    }

    /**
     * PASSO 2: Finalização e Confronto (Cego)
     */
    @Transactional
    public Recebimento finalizarConferencia(Long id) {
        Recebimento recebimento = recebimentoRepository.findByIdComItens(id)
                .orElseThrow(() -> new EntityNotFoundException("Recebimento não encontrado"));

        if (recebimento.getStatus() == StatusRecebimento.FINALIZADO) {
            throw new IllegalArgumentException("Recebimento já está finalizado!");
        }

        boolean divergente = false;

        // 1. Verifica item por item
        for (ItemRecebimento item : recebimento.getItens()) {
            // compareTo != 0 significa que os números são diferentes
            if (item.getQuantidadeConferida().compareTo(item.getQuantidadeNota()) != 0) {
                divergente = true;
                break; // Achou um erro, já basta
            }
        }

        // 2. Atualiza Status
        if (divergente) {
            recebimento.setStatus(StatusRecebimento.DIVERGENTE);
        } else {
            recebimento.setStatus(StatusRecebimento.FINALIZADO);
            // Aqui futuramente chamaremos o EstoqueService.adicionarEstoque(recebimento)
        }

        // 3. Marca a data de fim (se necessário criamos o campo no banco depois, por
        // enquanto usamos data atualização)
        // recebimento.setDataFinalizacao(LocalDateTime.now()); // Se tiver criado a
        // coluna no banco

        return recebimentoRepository.save(recebimento);
    }

    @Transactional
    public void cancelarConferencia(Long id) {
        Recebimento recebimento = recebimentoRepository.findByIdComItens(id)
                .orElseThrow(() -> new EntityNotFoundException("Recebimento não encontrado"));

        if (recebimento.getStatus() == StatusRecebimento.FINALIZADO) {
            throw new IllegalArgumentException("Não é possível cancelar um recebimento já finalizado!");
        }

        // 1. Apaga todos os volumes (LPNs) gerados
        volumeRepository.deleteByRecebimentoId(id);

        // 2. Zera a contagem de todos os itens
        for (ItemRecebimento item : recebimento.getItens()) {
            item.setQuantidadeConferida(BigDecimal.ZERO);
        }

        // 3. Reseta o Status
        recebimento.setStatus(StatusRecebimento.AGUARDANDO);

        recebimentoRepository.save(recebimento);
    }

    // --- Métodos Auxiliares ---

    private String gerarLpnUnico(Long recebimentoId, Produto produto) {
        // Formato Solicitado: {DepID}{ProdID}-{RecID}-{Nano}
        // Ex: Depositante 1, Produto 500, Rec 10 -> "1500-10-829381203"

        Long depositanteId = produto.getDepositante().getId();
        Long produtoId = produto.getId();

        String prefixo = depositanteId + "" + produtoId + "-" + recebimentoId + "-";

        // Gera um UUID (ex: 550e8400-e29b...) e pega só os primeiros 4 caracteres
        String sufixo = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        String lpnGerado = prefixo + sufixo;

        // Validação de segurança (Loop de colisão - muito raro, mas boa prática)
        // Se por um acaso do destino gerar um repetido, ele tenta de novo
        while (volumeRepository.existsByLpn(lpnGerado)) {
            sufixo = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            lpnGerado = prefixo + sufixo;
        }

        return lpnGerado;
    }
}