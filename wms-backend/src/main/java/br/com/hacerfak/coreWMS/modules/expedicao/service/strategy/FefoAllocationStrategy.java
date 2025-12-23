package br.com.hacerfak.coreWMS.modules.expedicao.service.strategy;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import br.com.hacerfak.coreWMS.modules.estoque.repository.EstoqueSaldoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component("FEFO")
@RequiredArgsConstructor
public class FefoAllocationStrategy implements AlocacaoStrategy {

    private final EstoqueSaldoRepository saldoRepository;

    @Override
    public List<EstoqueSaldo> buscarSaldosCandidatos(Produto produto, BigDecimal quantidadeNecessaria) {
        // Implementação do método que já criamos no repositório:
        // Busca saldos onde (Qtd - Reservado) > 0, ordenado por data de validade ASC
        return saldoRepository.buscarDisponiveisPorValidade(produto.getId());
    }
}