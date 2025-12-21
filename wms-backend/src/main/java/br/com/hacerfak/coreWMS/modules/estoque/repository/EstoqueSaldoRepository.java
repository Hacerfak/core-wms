package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import jakarta.persistence.LockModeType; // Importante
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock; // Importante
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueSaldoRepository extends JpaRepository<EstoqueSaldo, Long> {

      @Query("SELECT SUM(e.quantidade) FROM EstoqueSaldo e WHERE e.produto.id = :produtoId")
      Double somarEstoqueDoProduto(@Param("produtoId") Long produtoId);

      @Query("""
                     SELECT e FROM EstoqueSaldo e
                     WHERE e.produto.id = :produtoId
                     AND (e.quantidade - COALESCE(e.quantidadeReservada, 0)) > 0
                     AND e.localizacao.bloqueado = false
                     AND e.localizacao.ativo = true
                     ORDER BY e.dataValidade ASC NULLS LAST, e.id ASC
                  """)
      List<EstoqueSaldo> buscarDisponiveisPorValidade(@Param("produtoId") Long produtoId);

      // Traz Produto e Localização junto para evitar o LazyInitializationException
      @Query("SELECT e FROM EstoqueSaldo e JOIN FETCH e.produto JOIN FETCH e.localizacao")
      List<EstoqueSaldo> findAllCompleto();

      // --- CORREÇÃO DE CONCORRÊNCIA ---
      // @Lock(LockModeType.PESSIMISTIC_WRITE) garante que o banco trave essa linha
      // para leitura e escrita até o fim da transação.
      @Lock(LockModeType.PESSIMISTIC_WRITE)
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
                  @Param("lpn") String lpn,
                  @Param("lote") String lote,
                  @Param("serial") String serial);
}