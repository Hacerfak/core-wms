package br.com.hacerfak.coreWMS.modules.seguranca.repository;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Mudamos de UserDetails para Optional<Usuario> para ter acesso à lista de
    // empresas
    Optional<Usuario> findByLogin(String login);

    // --- NOVO MÉTODO OTIMIZADO ---
    // Traz o usuário e seus acessos em UM único SELECT (evita N+1)
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.acessos a LEFT JOIN FETCH a.empresa WHERE u.login = :login")
    Optional<Usuario> findByLoginWithAcessos(@Param("login") String login);
}