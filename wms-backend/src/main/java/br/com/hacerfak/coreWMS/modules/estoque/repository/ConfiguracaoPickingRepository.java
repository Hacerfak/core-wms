package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.ConfiguracaoPicking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfiguracaoPickingRepository extends JpaRepository<ConfiguracaoPicking, Long> {
    List<ConfiguracaoPicking> findByAtivoTrue();

    Optional<ConfiguracaoPicking> findByProdutoIdAndLocalizacaoId(Long produtoId, Long localizacaoId);
}