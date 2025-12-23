package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TarefaMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaMovimentacaoRepository extends JpaRepository<TarefaMovimentacao, Long> {
    List<TarefaMovimentacao> findByStatus(StatusTarefa status);

    // Evitar duplicidade: Já existe tarefa pendente para este destino?
    boolean existsByDestinoIdAndStatus(Long destinoId, StatusTarefa status);
}