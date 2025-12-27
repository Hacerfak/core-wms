package br.com.hacerfak.coreWMS.modules.operacao.repository;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.operacao.domain.TarefaDivergencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaDivergenciaRepository extends JpaRepository<TarefaDivergencia, Long> {
    List<TarefaDivergencia> findBySolicitacaoId(Long solicitacaoId);

    List<TarefaDivergencia> findByStatus(StatusTarefa status);
}