package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.Localizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocalizacaoRepository extends JpaRepository<Localizacao, Long> {

    // Método Scan (Coletor)
    Optional<Localizacao> findByEnderecoCompleto(String enderecoCompleto);

    // Método Listagem por Área
    List<Localizacao> findByAreaId(Long areaId);

    // Método Legado (Opcional, se algum código antigo ainda usar)
    Optional<Localizacao> findByCodigo(String codigo);
}