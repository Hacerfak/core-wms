package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.EstoqueSaldo;
import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusQualidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueSaldoRepository extends JpaRepository<EstoqueSaldo, Long> {

   @Query("SELECT SUM(e.quantidade) FROM EstoqueSaldo e WHERE e.produto.id = :produtoId")
   Double somarEstoqueDoProduto(@Param("produtoId") Long produtoId);

   // Busca apenas saldo DISPONÍVEL para venda (ignora avaria/bloqueado)
   @Query("""
            SELECT e FROM EstoqueSaldo e
            WHERE e.produto.id = :produtoId
            AND (e.quantidade - COALESCE(e.quantidadeReservada, 0)) > 0
            AND e.localizacao.bloqueado = false
            AND e.localizacao.ativo = true
            AND e.statusQualidade = 'DISPONIVEL'
            ORDER BY e.dataValidade ASC NULLS LAST, e.id ASC
         """)
   List<EstoqueSaldo> buscarDisponiveisPorValidade(@Param("produtoId") Long produtoId);

   @Query("SELECT e FROM EstoqueSaldo e JOIN FETCH e.produto JOIN FETCH e.localizacao")
   List<EstoqueSaldo> findAllCompleto();

   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("""
            SELECT e FROM EstoqueSaldo e
            WHERE e.produto.id = :produtoId
            AND e.localizacao.id = :localId
            AND (:lpn IS NULL OR e.lpn = :lpn)
            AND (:lote IS NULL OR e.lote = :lote)
            AND (:serial IS NULL OR e.numeroSerie = :serial)
            AND (:statusQualidade IS NULL OR e.statusQualidade = :statusQualidade)
         """)
   Optional<EstoqueSaldo> buscarSaldoExato(
         @Param("produtoId") Long produtoId,
         @Param("localId") Long localId,
         @Param("lpn") String lpn,
         @Param("lote") String lote,
         @Param("serial") String serial,
         @Param("statusQualidade") StatusQualidade statusQualidade);

   // Sobrecarga para manter compatibilidade onde não se passa qualidade (assume
   // nulo na query acima)
   default Optional<EstoqueSaldo> buscarSaldoExato(Long pId, Long lId, String lpn, String lote, String serial) {
      return buscarSaldoExato(pId, lId, lpn, lote, serial, null);
   }

   // FIFO (First-In, First-Out): O mais antigo sai primeiro (ORDER BY id ASC ou
   // dataCriacao ASC)
   @Query("""
            SELECT e FROM EstoqueSaldo e
            WHERE e.produto.id = :produtoId
            AND (e.quantidade - COALESCE(e.quantidadeReservada, 0)) > 0
            AND e.localizacao.bloqueado = false
            AND e.localizacao.ativo = true
            AND e.statusQualidade = 'DISPONIVEL'
            ORDER BY e.dataCriacao ASC, e.id ASC
         """)
   List<EstoqueSaldo> buscarDisponiveisPorAntiguidade(@Param("produtoId") Long produtoId);

   // LIFO (Last-In, First-Out): O mais novo sai primeiro (ORDER BY id DESC)
   @Query("""
            SELECT e FROM EstoqueSaldo e
            WHERE e.produto.id = :produtoId
            AND (e.quantidade - COALESCE(e.quantidadeReservada, 0)) > 0
            AND e.localizacao.bloqueado = false
            AND e.localizacao.ativo = true
            AND e.statusQualidade = 'DISPONIVEL'
            ORDER BY e.dataCriacao DESC, e.id DESC
         """)
   List<EstoqueSaldo> buscarDisponiveisPorRecencia(@Param("produtoId") Long produtoId);

   // Verifica se existe algum saldo deste produto com este serial e quantidade > 0
   @Query("""
            SELECT COUNT(e) > 0 FROM EstoqueSaldo e
            WHERE e.produto.id = :produtoId
            AND e.numeroSerie = :serial
            AND e.quantidade > 0
         """)
   boolean existsByProdutoIdAndNumeroSerie(@Param("produtoId") Long produtoId, @Param("serial") String serial);

   // SOMA TOTAL DO LOCAL (Para Inventário Geográfico/Cego Geral)
   @Query("SELECT COALESCE(SUM(e.quantidade), 0) FROM EstoqueSaldo e WHERE e.localizacao.id = :localId")
   java.math.BigDecimal somarQuantidadePorLocal(@Param("localId") Long localId);

   // SOMA DE PRODUTO ESPECÍFICO NO LOCAL (Para Inventário Rotativo/Focado)
   @Query("SELECT COALESCE(SUM(e.quantidade), 0) FROM EstoqueSaldo e WHERE e.localizacao.id = :localId AND e.produto.id = :produtoId")
   java.math.BigDecimal somarQuantidadePorLocalEProduto(@Param("localId") Long localId,
         @Param("produtoId") Long produtoId);
}