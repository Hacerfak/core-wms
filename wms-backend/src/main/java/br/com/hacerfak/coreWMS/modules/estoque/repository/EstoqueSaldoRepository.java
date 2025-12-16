package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueSaldoRepository extends JpaRepository<EstoqueSaldo, Long> {

      // Usado no Dashboard ou Lista Simples
      @Query("SELECT SUM(e.quantidade) FROM EstoqueSaldo e WHERE e.produto.id = :produtoId")
      Double somarEstoqueDoProduto(@Param("produtoId") Long produtoId);

      // --- USADO NO ALOCACAO SERVICE (FEFO) ---
      // Traz saldos onde (Qtd Fisica - Qtd Reservada) > 0
      // Ordena por Data de Validade ASC (Vence primeiro, sai primeiro)
      @Query("""
                     SELECT e FROM EstoqueSaldo e
                     WHERE e.produto.id = :produtoId
                     AND (e.quantidade - COALESCE(e.quantidadeReservada, 0)) > 0
                     AND e.localizacao.bloqueado = false
                     AND e.localizacao.ativo = true
                     ORDER BY e.dataValidade ASC NULLS LAST, e.id ASC
                  """)
      List<EstoqueSaldo> buscarDisponiveisPorValidade(@Param("produtoId") Long produtoId);

      // --- USADO NO PICKING SERVICE E ESTOQUE SERVICE ---
      // Busca exata considerando que Lote ou Serial podem ser NULL no banco
      // ATUALIZE ESTE MÉTODO
      @Query("""
                     SELECT e FROM EstoqueSaldo e
                     WHERE e.produto.id = :produtoId
                     AND e.localizacao.id = :localId
                     AND (:lpn IS NULL OR e.lpn = :lpn)
                     AND (:lote IS NULL OR e.lote = :lote)
                     AND (:serial IS NULL OR e.numeroSerie = :serial)
                  """)
      Optional<EstoqueSaldo> buscarSaldoExato(
                  @Param("produtoId") Long produtoId,
                  @Param("localId") Long localId,
                  @Param("lpn") String lpn, // <--- Novo parametro
                  @Param("lote") String lote,
                  @Param("serial") String serial);
}