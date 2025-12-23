package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.Lpn;
import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusLpn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface LpnRepository extends JpaRepository<Lpn, Long> {
    Optional<Lpn> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    // O Spring Data JPA cria a query automaticamente baseado no nome do atributo
    // 'solicitacaoEntradaId' na entidade Lpn
    List<Lpn> findBySolicitacaoEntradaIdAndStatus(Long solicitacaoEntradaId, StatusLpn status);
}