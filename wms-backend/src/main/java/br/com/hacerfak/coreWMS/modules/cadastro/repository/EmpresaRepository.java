package br.com.hacerfak.coreWMS.modules.cadastro.repository;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    // Para validar se a empresa já está cadastrada no onboarding
    boolean existsByCnpj(String cnpj);

    // Para encontrar a configuração de conexão pelo ID do Tenant
    Optional<Empresa> findByTenantId(String tenantId);

    // Para buscar dados da empresa pelo CNPJ (login com certificado)
    Optional<Empresa> findByCnpj(String cnpj);
}