package br.com.hacerfak.coreWMS.modules.sistema.repository;

import br.com.hacerfak.coreWMS.modules.sistema.domain.SistemaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SistemaConfigRepository extends JpaRepository<SistemaConfig, String> {
}