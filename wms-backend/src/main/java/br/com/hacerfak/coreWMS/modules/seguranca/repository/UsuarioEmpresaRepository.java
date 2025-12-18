package br.com.hacerfak.coreWMS.modules.seguranca.repository;

import br.com.hacerfak.coreWMS.modules.seguranca.domain.UsuarioEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, Long> {

    // --- CORREÇÃO DO ERRO 500/LAZY ---
    // Usamos JOIN FETCH para garantir que o objeto Usuario venha preenchido
    // na mesma consulta, evitando que o Hibernate tente buscar depois com a conexão
    // fechada.
    @Query("SELECT ue FROM UsuarioEmpresa ue JOIN FETCH ue.usuario WHERE ue.empresa.id = :empresaId")
    List<UsuarioEmpresa> findByEmpresaId(@Param("empresaId") Long empresaId);

    boolean existsByUsuarioIdAndEmpresaId(Long usuarioId, Long empresaId);
}