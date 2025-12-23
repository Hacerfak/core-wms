package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import br.com.hacerfak.coreWMS.modules.estoque.domain.TipoLocalizacao;

import org.springframework.cache.annotation.Cacheable; // <--- Importante
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocalizacaoRepository extends JpaRepository<Localizacao, Long> {

    // Cache para Scan
    @Cacheable(value = "locais", key = "#enderecoCompleto")
    Optional<Localizacao> findByEnderecoCompleto(String enderecoCompleto);

    List<Localizacao> findByAreaId(Long areaId);

    // Cache para busca por ID
    @Cacheable(value = "locais", key = "#id")
    Optional<Localizacao> findById(Long id);

    Optional<Localizacao> findByCodigo(String codigo);

    Optional<Localizacao> findFirstByTipoAndAtivoTrue(TipoLocalizacao tipo);
}