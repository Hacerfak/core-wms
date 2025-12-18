package br.com.hacerfak.coreWMS.modules.seguranca.repository;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long> {
    List<UsuarioEmpresa> findByEmpresaId(Long empresaId);

    boolean existsByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId);
}