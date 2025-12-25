package br.com.hacerfak.coreWMS.modules.impressao.repository;

import br.com.hacerfak.coreWMS.modules.impressao.domain.FilaImpressao;
import br.com.hacerfak.coreWMS.modules.impressao.domain.StatusImpressao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilaImpressaoRepository extends JpaRepository<FilaImpressao, Long> {

    // --- CORREÇÃO DO LAZY LOADING ---
    // Isso força o Hibernate a fazer um JOIN FETCH com a impressora,
    // trazendo os dados antes de fechar a sessão.
    @Override
    @EntityGraph(attributePaths = { "impressoraAlvo" })
    Page<FilaImpressao> findAll(Pageable pageable);

    // Método usado pelo AGENTE para buscar trabalho
    @Query("SELECT f FROM FilaImpressao f JOIN FETCH f.impressoraAlvo WHERE f.status = 'PENDENTE' ORDER BY f.dataCriacao ASC")
    List<FilaImpressao> buscarPendentesComImpressora();

    // Filtro por impressora específica (caso tenhamos múltiplos agentes)
    List<FilaImpressao> findByStatusAndImpressoraAlvoId(StatusImpressao status, Long impressoraId);

    // --- CORREÇÃO PARA O AGENTE (POLLING) ---
    // Sobrescreve a busca por ID para trazer a impressora junto (Eagerly)
    @Override
    @EntityGraph(attributePaths = { "impressoraAlvo" })
    Optional<FilaImpressao> findById(Long id);
}