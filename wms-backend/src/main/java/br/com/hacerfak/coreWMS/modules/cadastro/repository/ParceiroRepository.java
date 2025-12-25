package br.com.hacerfak.coreWMS.modules.cadastro.repository;

import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParceiroRepository extends JpaRepository<Parceiro, Long> {
    Optional<Parceiro> findByCpfCnpj(String cpfCnpj);
}
