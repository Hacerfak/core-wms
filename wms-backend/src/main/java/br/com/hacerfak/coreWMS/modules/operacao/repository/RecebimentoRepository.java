package br.com.hacerfak.coreWMS.modules.operacao.repository;

import br.com.hacerfak.coreWMS.modules.operacao.domain.Recebimento;
import br.com.hacerfak.coreWMS.modules.operacao.domain.StatusRecebimento;
import br.com.hacerfak.coreWMS.modules.operacao.dto.RecebimentoResumoDTO;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecebimentoRepository extends JpaRepository<Recebimento, Long> {

    /**
     * Busca um recebimento pela Chave de Acesso da NFe (44 dígitos).
     * Essencial para quando o operador bipa o código de barras da DANFE na doca.
     */
    Optional<Recebimento> findByChaveAcesso(String chaveAcesso);

    /**
     * Verifica se a nota já foi importada antes de tentar processar o XML
     * novamente.
     * Evita duplicidade de entrada.
     */
    boolean existsByChaveAcesso(String chaveAcesso);

    /**
     * Busca recebimentos pelo status.
     * Útil para o Dashboard: "Mostrar todas as notas AGUARDANDO conferência".
     */
    List<Recebimento> findByStatus(StatusRecebimento status);

    /**
     * Busca pelo número da nota e fornecedor (caso a chave de acesso não esteja
     * legível).
     */
    Optional<Recebimento> findByNumNotaFiscalAndFornecedor(String numNotaFiscal, String fornecedor);

    // A anotação @EntityGraph força o carregamento dos 'itens' numa única query
    // (EAGER)
    @EntityGraph(attributePaths = { "itens", "itens.produto" })
    List<Recebimento> findAll();

    // NOVO: Busca Otimizada (Projeção)
    @Query("SELECT new br.com.hacerfak.coreWMS.modules.operacao.dto.RecebimentoResumoDTO(" +
            "r.id, r.numNotaFiscal, r.fornecedor, r.dataCriacao, r.dataEmissao, r.status) " +
            "FROM Recebimento r ORDER BY r.id DESC")
    List<RecebimentoResumoDTO> findAllResumo();

    // NOVO: Busca completa para a tela de Detalhes
    @Query("SELECT r FROM Recebimento r LEFT JOIN FETCH r.itens i LEFT JOIN FETCH i.produto p WHERE r.id = :id")
    Optional<Recebimento> findByIdComItens(@Param("id") Long id);

}
