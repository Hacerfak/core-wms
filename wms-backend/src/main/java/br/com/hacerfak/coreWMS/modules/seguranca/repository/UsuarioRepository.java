package br.com.hacerfak.coreWMS.modules.seguranca.repository;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    // Solução com EntityGraph: Carrega 'acessos' e 'acessos.empresa'
    // automaticamente
    @EntityGraph(attributePaths = { "acessos", "acessos.empresa" })
    @Query("SELECT u FROM Usuario u WHERE u.login = :login")
    Optional<Usuario> findByLoginWithAcessos(@Param("login") String login);
}