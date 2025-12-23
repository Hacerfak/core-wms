package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TarefaArmazenagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaArmazenagemRepository extends JpaRepository<TarefaArmazenagem, Long> {
    // Para listar no coletor
    List<TarefaArmazenagem> findByStatus(StatusTarefa status);

    // Para evitar duplicidade na geração
    boolean existsByLpnIdAndStatus(Long lpnId, StatusTarefa status);
}