package br.com.hacerfak.coreWMS.modules.operacao.repository;

import br.com.hacerfak.coreWMS.modules.operacao.domain.SolicitacaoEntrada;
import br.com.hacerfak.coreWMS.modules.operacao.dto.SolicitacaoEntradaResumoDTO;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoEntradaRepository extends JpaRepository<SolicitacaoEntrada, Long> {

    @EntityGraph(attributePaths = { "itens", "fornecedor" })
    Optional<SolicitacaoEntrada> findById(Long id);

    boolean existsByChaveAcesso(String chaveAcesso);

    // Consulta Otimizada para Dashboard
    @Query("""
               SELECT new br.com.hacerfak.coreWMS.modules.operacao.dto.SolicitacaoEntradaResumoDTO(
                   s.id,
                   s.codigoExterno,
                   s.fornecedor.nome,
                   s.status,
                   s.dataCriacao,
                   s.dataEmissao
               )
               FROM SolicitacaoEntrada s
               ORDER BY s.id DESC
            """)
    List<SolicitacaoEntradaResumoDTO> findAllResumo();
}