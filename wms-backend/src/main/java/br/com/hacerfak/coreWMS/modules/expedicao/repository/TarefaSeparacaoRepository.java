package br.com.hacerfak.coreWMS.modules.expedicao.repository;

import br.com.hacerfak.coreWMS.modules.expedicao.domain.TarefaSeparacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarefaSeparacaoRepository extends JpaRepository<TarefaSeparacao, Long> {

    /**
     * Retorna todas as tarefas de um pedido.
     */
    List<TarefaSeparacao> findByPedidoId(Long pedidoId);

    /**
     * Retorna apenas o que falta fazer em um pedido.
     * Útil para o operador saber o progresso.
     */
    List<TarefaSeparacao> findByPedidoIdAndConcluidaFalse(Long pedidoId);

    /**
     * (Opcional) Retorna tarefas pendentes por zona/rua.
     * Útil se você quiser filtrar para o operador só ver tarefas da "Rua A".
     * Exige JOIN com Localizacao, mas o Spring Data JPA resolve se mapeado
     * corretamente.
     */
    // List<TarefaSeparacao>
    // findByLocalizacaoOrigemCodigoStartingWithAndConcluidaFalse(String
    // prefixoRua);
}
