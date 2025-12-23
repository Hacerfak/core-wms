package br.com.hacerfak.coreWMS.modules.faturamento.repository;

import br.com.hacerfak.coreWMS.modules.faturamento.domain.ApontamentoServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ApontamentoServicoRepository extends JpaRepository<ApontamentoServico, Long> {
    List<ApontamentoServico> findByClienteIdAndDataReferenciaBetween(Long clienteId, LocalDate inicio, LocalDate fim);
}