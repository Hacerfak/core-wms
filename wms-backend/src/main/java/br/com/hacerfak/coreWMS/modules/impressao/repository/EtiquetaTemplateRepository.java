package br.com.hacerfak.coreWMS.modules.impressao.repository;

import br.com.hacerfak.coreWMS.modules.impressao.domain.EtiquetaTemplate;
import br.com.hacerfak.coreWMS.modules.impressao.domain.TipoFinalidadeEtiqueta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EtiquetaTemplateRepository extends JpaRepository<EtiquetaTemplate, Long> {

    // Busca o template padrão para aquele tipo (caso o usuário não escolha um
    // específico)
    Optional<EtiquetaTemplate> findFirstByTipoFinalidadeAndPadraoTrue(TipoFinalidadeEtiqueta tipoFinalidade);

    // TODO: Busca templates específicos de um depositante
    // List<EtiquetaTemplate> findByDepositanteId(Long depositanteId);
}