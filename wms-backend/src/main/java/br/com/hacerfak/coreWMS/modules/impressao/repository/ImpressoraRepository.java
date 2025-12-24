package br.com.hacerfak.coreWMS.modules.impressao.repository;

import br.com.hacerfak.coreWMS.modules.impressao.domain.Impressora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImpressoraRepository extends JpaRepository<Impressora, Long> {
    Optional<Impressora> findByNome(String nome);

    List<Impressora> findByAtivoTrue();

    List<Impressora> findByArmazemIdAndAtivoTrue(Long armazemId);
}