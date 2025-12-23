package br.com.hacerfak.coreWMS.modules.expedicao.repository;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.TarefaSeparacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaSeparacaoRepository extends JpaRepository<TarefaSeparacao, Long> {

    // Para o coletor: Tarefas pendentes de uma onda
    List<TarefaSeparacao> findByOndaIdAndStatus(Long ondaId, StatusTarefa status);

    // Todas as tarefas pendentes
    List<TarefaSeparacao> findByStatus(StatusTarefa status);
}