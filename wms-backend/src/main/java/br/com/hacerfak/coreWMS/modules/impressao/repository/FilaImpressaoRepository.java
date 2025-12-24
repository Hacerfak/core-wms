package br.com.hacerfak.coreWMS.modules.impressao.repository;

import br.com.hacerfak.coreWMS.modules.impressao.domain.FilaImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.domain.StatusImpressao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilaImpressaoRepository extends JpaRepository<FilaImpressao, Long> {

    // Método usado pelo AGENTE para buscar trabalho
    @Query("SELECT f FROM FilaImpressao f JOIN FETCH f.impressoraAlvo WHERE f.status = 'PENDENTE' ORDER BY f.dataCriacao ASC")
    List<FilaImpressao> buscarPendentesComImpressora();

    // Filtro por impressora específica (caso tenhamos múltiplos agentes)
    List<FilaImpressao> findByStatusAndImpressoraAlvoId(StatusImpressao status, Long impressoraId);
}