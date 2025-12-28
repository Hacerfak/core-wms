package br.com.hacerfak.coreWMS.modules.operacao.repository;

import br.com.hacerfak.coreWMS.modules.operacao.domain.SolicitacaoEntrada;
import br.com.hacerfak.coreWMS.modules.operacao.dto.SolicitacaoEntradaResumoDTO;
import br.com.hacerfak.coreWMS.modules.operacao.dto.ProgressoRecebimentoDTO;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoEntradaRepository extends JpaRepository<SolicitacaoEntrada, Long> {

    @EntityGraph(attributePaths = { "itens", "itens.produto", "fornecedor", "doca" })
    Optional<SolicitacaoEntrada> findById(Long id);

    boolean existsByChaveAcesso(String chaveAcesso);

    // Consulta Otimizada para Dashboard
    @Query("""
               SELECT new br.com.hacerfak.coreWMS.modules.operacao.dto.SolicitacaoEntradaResumoDTO(
                   s.id,
                   s.codigoExterno,
                   s.notaFiscal,
                   s.chaveAcesso,
                   s.fornecedor.nome,
                   s.status,
                   s.dataCriacao,
                   s.dataEmissao,
                   d.id,
                   d.enderecoCompleto,
                   a.id,
                   a.status,
                   a.placaVeiculo
               )
               FROM SolicitacaoEntrada s
               LEFT JOIN s.agendamento a
               LEFT JOIN s.doca d
               ORDER BY s.id DESC
            """)
    List<SolicitacaoEntradaResumoDTO> findAllResumo();

    @Query("""
                SELECT new br.com.hacerfak.coreWMS.modules.operacao.dto.ProgressoRecebimentoDTO(
                    s.id,
                    SUM(i.quantidadePrevista),
                    SUM(i.quantidadeConferida)
                )
                FROM SolicitacaoEntrada s
                JOIN s.itens i
                WHERE s.id = :id
                GROUP BY s.id
            """)
    Optional<ProgressoRecebimentoDTO> buscarProgresso(@Param("id") Long id);
}