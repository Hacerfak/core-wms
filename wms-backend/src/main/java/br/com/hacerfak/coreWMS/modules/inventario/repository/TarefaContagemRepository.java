package br.com.hacerfak.coreWMS.modules.inventario.repository;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.inventario.domain.TarefaContagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaContagemRepository extends JpaRepository<TarefaContagem, Long> {
    List<TarefaContagem> findByInventarioId(Long inventarioId);

    // Para o coletor
    List<TarefaContagem> findByStatus(StatusTarefa status);
}