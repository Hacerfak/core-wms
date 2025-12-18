package br.com.hacerfak.coreWMS.modules.seguranca.repository;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioPerfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioPerfilRepository extends JpaRepository<UsuarioPerfil, Long> {
    List<UsuarioPerfil> findByUsuarioId(Long usuarioId);

    Optional<UsuarioPerfil> findByUsuarioIdAndPerfilId(Long usuarioId, Long perfilId);
}