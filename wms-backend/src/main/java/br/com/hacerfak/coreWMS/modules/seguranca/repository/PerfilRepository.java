package br.com.hacerfak.coreWMS.modules.seguranca.repository;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {
}