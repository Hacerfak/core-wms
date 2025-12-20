package br.com.hacerfak.coreWMS.modules.cadastro.repository;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.EmpresaDados;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaDadosRepository extends JpaRepository<EmpresaDados, Long> {
}