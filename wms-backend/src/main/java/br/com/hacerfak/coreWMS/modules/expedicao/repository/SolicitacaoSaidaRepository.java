package br.com.hacerfak.coreWMS.modules.expedicao.repository;

import br.com.hacerfak.coreWMS.core.domain.workflow.StatusSolicitacao;
import br.com.hacerfak.coreWMS.modules.expedicao.domain.SolicitacaoSaida;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface SolicitacaoSaidaRepository extends JpaRepository<SolicitacaoSaida, Long> {

    @EntityGraph(attributePaths = { "itens", "cliente" })
    Optional<SolicitacaoSaida> findById(Long id);

    boolean existsByCodigoExterno(String codigoExterno);

    List<SolicitacaoSaida> findByStatus(StatusSolicitacao status);
}