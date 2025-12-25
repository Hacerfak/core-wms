package br.com.hacerfak.coreWMS.modules.estoque.repository;

import org.springframework.cache.annotation.Cacheable;
import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoLocalizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalizacaoRepository extends JpaRepository<Localizacao, Long> {

        // Cache para Scan
        @Cacheable(value = "locais", key = "#enderecoCompleto")
        Optional<Localizacao> findByEnderecoCompleto(String enderecoCompleto);

        List<Localizacao> findByAreaId(Long areaId);

        // Lista tudo ativo
        List<Localizacao> findByAtivoTrue();

        // NOVO: Busca global por tipo (ex: trazer todas as DOCAS do sistema)
        List<Localizacao> findByTipoAndAtivoTrue(TipoLocalizacao tipo);

        // Cache para busca por ID
        @Cacheable(value = "locais", key = "#id")
        Optional<Localizacao> findById(Long id);

        Optional<Localizacao> findByCodigo(String codigo);

        Optional<Localizacao> findFirstByTipoAndAtivoTrue(TipoLocalizacao tipo);

        // SLOTTING: Busca endereços onde o produto JÁ existe (Consolidação)
        @Query("SELECT DISTINCT s.localizacao FROM EstoqueSaldo s " +
                        "WHERE s.produto.id = :produtoId " +
                        "AND s.localizacao.tipo = 'ARMAZENAGEM' " +
                        "AND s.localizacao.bloqueado = false " +
                        "ORDER BY s.localizacao.id ASC")
        List<Localizacao> findLocaisComProduto(@Param("produtoId") Long produtoId);

        // SLOTTING: Busca endereços VAZIOS em área de Armazenagem
        // (Lógica simples: não tem registro na tabela de saldo com qtd > 0)
        @Query("SELECT l FROM Localizacao l " +
                        "WHERE l.tipo = 'ARMAZENAGEM' " +
                        "AND l.bloqueado = false " +
                        "AND l.ativo = true " +
                        "AND NOT EXISTS (SELECT s FROM EstoqueSaldo s WHERE s.localizacao = l AND s.quantidade > 0) " +
                        "ORDER BY l.enderecoCompleto ASC")
        List<Localizacao> findLocaisVazios();
}