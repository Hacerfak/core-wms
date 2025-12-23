package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.ConfiguracaoPicking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConfiguracaoPickingRepository extends JpaRepository<ConfiguracaoPicking, Long> {
    List<ConfiguracaoPicking> findByAtivoTrue();
}