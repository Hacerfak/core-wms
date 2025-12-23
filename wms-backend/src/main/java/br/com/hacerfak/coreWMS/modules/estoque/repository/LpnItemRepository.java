package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.LpnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LpnItemRepository extends JpaRepository<LpnItem, Long> {
    // Busca um item específico dentro de uma LPN (para somar quantidade se bipar de
    // novo)
    Optional<LpnItem> findByLpnIdAndProdutoIdAndLote(Long lpnId, Long produtoId, String lote);

    boolean existsByProdutoIdAndNumeroSerieAndLpnCodigoNot(Long produtoId, String numeroSerie, String codigoLpnIgnorar);
}