package br.com.hacerfak.coreWMS.modules.faturamento.repository;

import br.com.hacerfak.coreWMS.modules.faturamento.domain.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long> {
    Optional<Servico> findByCodigo(String codigo);
}