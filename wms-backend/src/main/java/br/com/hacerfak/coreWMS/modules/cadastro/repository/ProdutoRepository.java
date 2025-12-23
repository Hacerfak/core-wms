package br.com.hacerfak.coreWMS.modules.cadastro.repository;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Produto;
import org.springframework.cache.annotation.Cacheable; // <--- Importante
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findBySkuAndDepositante(String sku, Parceiro depositante);

    boolean existsBySku(String sku);

    // --- Cache para busca por ID ---
    @Cacheable(value = "produtos", key = "#id")
    Optional<Produto> findById(Long id);

    // --- Cache para a super busca ---
    @Cacheable(value = "produtos", key = "#codigo")
    @Query("SELECT p FROM Produto p WHERE p.sku = :codigo OR p.ean13 = :codigo OR p.dun14 = :codigo")
    Optional<Produto> findByCodigoBarras(@Param("codigo") String codigo);
}