package br.com.hacerfak.coreWMS.modules.operacao.repository;

import br.com.hacerfak.coreWMS.modules.operacao.domain.ItemSolicitacaoEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ItemSolicitacaoEntradaRepository extends JpaRepository<ItemSolicitacaoEntrada, Long> {

    // OTIMIZAÇÃO: Atualiza direto no banco, sem carregar o objeto em memória
    @Modifying
    @Query("UPDATE ItemSolicitacaoEntrada i SET i.quantidadeConferida = i.quantidadeConferida + :qtd " +
            "WHERE i.solicitacao.id = :solicitacaoId AND i.produto.id = :produtoId")
    void somarQuantidadeConferida(@Param("solicitacaoId") Long solicitacaoId,
            @Param("produtoId") Long produtoId,
            @Param("qtd") BigDecimal qtd);

    // OTIMIZAÇÃO: Para estorno
    @Modifying
    @Query("UPDATE ItemSolicitacaoEntrada i SET i.quantidadeConferida = i.quantidadeConferida - :qtd " +
            "WHERE i.solicitacao.id = :solicitacaoId AND i.produto.id = :produtoId")
    void subtrairQuantidadeConferida(@Param("solicitacaoId") Long solicitacaoId,
            @Param("produtoId") Long produtoId,
            @Param("qtd") BigDecimal qtd);
}