package br.com.hacerfak.coreWMS.modules.cadastro.repository;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.EmpresaConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaConfigRepository extends JpaRepository<EmpresaConfig, Long> {
}