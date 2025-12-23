package br.com.hacerfak.coreWMS.modules.operacao.repository;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusTarefa;
import br.com.hacerfak.coreWMS.modules.operacao.domain.TarefaConferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaConferenciaRepository extends JpaRepository<TarefaConferencia, Long> {

    // Todas as tarefas com status X
    List<TarefaConferencia> findByStatus(StatusTarefa status);

    // Tarefas atribuídas a um usuário específico
    List<TarefaConferencia> findByUsuarioAtribuidoAndStatus(String usuarioAtribuido, StatusTarefa status);

    // Tarefas sem atribuição (livres)
    List<TarefaConferencia> findByUsuarioAtribuidoIsNullAndStatus(StatusTarefa status);
}