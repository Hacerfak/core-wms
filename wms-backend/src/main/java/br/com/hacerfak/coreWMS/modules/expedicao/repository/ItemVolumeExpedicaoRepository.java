package br.com.hacerfak.coreWMS.modules.expedicao.repository;

import br.com.hacerfak.coreWMS.modules.expedicao.domain.ItemVolumeExpedicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ItemVolumeExpedicaoRepository extends JpaRepository<ItemVolumeExpedicao, Long> {

    // Soma a quantidade deste produto em TODOS os volumes deste pedido
    @Query("""
               SELECT COALESCE(SUM(i.quantidade), 0)
               FROM ItemVolumeExpedicao i
               WHERE i.volume.solicitacao.id = :solicitacaoId
               AND i.produto.id = :produtoId
            """)
    BigDecimal somarTotalEmbaladoPorProduto(
            @Param("solicitacaoId") Long solicitacaoId,
            @Param("produtoId") Long produtoId);
}