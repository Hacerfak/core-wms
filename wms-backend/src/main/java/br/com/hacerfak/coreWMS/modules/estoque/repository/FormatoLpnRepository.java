package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.FormatoLpn;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoSuporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormatoLpnRepository extends JpaRepository<FormatoLpn, Long> {

    boolean existsByCodigo(String codigo);

    // Para validação na edição (ignorar o próprio ID)
    boolean existsByCodigoAndIdNot(String codigo, Long id);

    Optional<FormatoLpn> findByCodigo(String codigo);

    List<FormatoLpn> findByAtivoTrue();

    List<FormatoLpn> findByTipoBaseAndAtivoTrue(TipoSuporte tipoBase);
}