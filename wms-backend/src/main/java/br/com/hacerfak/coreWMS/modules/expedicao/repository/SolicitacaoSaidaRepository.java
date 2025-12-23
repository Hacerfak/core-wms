package br.com.hacerfak.coreWMS.modules.expedicao.repository;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusSolicitacao;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.SolicitacaoSaida;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface SolicitacaoSaidaRepository extends JpaRepository<SolicitacaoSaida, Long> {

    @EntityGraph(attributePaths = { "itens", "cliente" })
    Optional<SolicitacaoSaida> findById(Long id);

    boolean existsByCodigoExterno(String codigoExterno);

    List<SolicitacaoSaida> findByStatus(StatusSolicitacao status);

    // --- NOVO MÉTODO PARA ROTEIRIZAÇÃO ---
    List<SolicitacaoSaida> findByStatusAndRota(StatusSolicitacao status, String rota);

    @Query("""
               SELECT COUNT(i) > 0
               FROM ItemSolicitacaoSaida i
               WHERE i.produto.id = :produtoId
               AND i.solicitacao.status IN ('CRIADA', 'EM_PROCESSAMENTO')
               AND (i.quantidadeSolicitada - i.quantidadeAlocada) > 0
            """)
    boolean existeDemandaPendenteParaProduto(@Param("produtoId") Long produtoId);
}