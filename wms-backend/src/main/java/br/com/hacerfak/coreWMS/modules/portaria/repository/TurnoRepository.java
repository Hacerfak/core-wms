package br.com.hacerfak.coreWMS.modules.portaria.repository;

import br.com.hacerfak.coreWMS.modules.portaria.domain.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {
    List<Turno> findByAtivoTrue();
}