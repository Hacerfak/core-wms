package br.com.hacerfak.coreWMS.modules.faturamento.repository;

import br.com.hacerfak.coreWMS.modules.faturamento.domain.AcordoPreco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcordoPrecoRepository extends JpaRepository<AcordoPreco, Long> {
    Optional<AcordoPreco> findByClienteIdAndServicoCodigo(Long clienteId, String codigoServico);
}