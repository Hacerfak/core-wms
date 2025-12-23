package br.com.hacerfak.coreWMS.modules.expedicao.service.strategy;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;

import java.math.BigDecimal;
import java.util.List;

public interface AlocacaoStrategy {
    /**
     * Dado um produto e uma quantidade necessária, retorna a lista de saldos
     * ordenados pela regra de negócio (ex: Vence Primeiro Sai Primeiro).
     */
    List<EstoqueSaldo> buscarSaldosCandidatos(Produto produto, BigDecimal quantidadeNecessaria);
}