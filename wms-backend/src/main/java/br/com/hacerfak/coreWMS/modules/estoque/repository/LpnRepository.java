package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.Lpn;
import br.com.hacerfak.coreWMS.modules.estoque.domain.StatusLpn;

import org.springframework.data.jpa.repository.EntityGraph; // <--- Importante
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface LpnRepository extends JpaRepository<Lpn, Long> {

    // Carrega itens e produto ao buscar por código (usado na bipagem)
    @EntityGraph(attributePaths = { "itens", "itens.produto", "itens.produto.depositante", "localizacaoAtual" })
    Optional<Lpn> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    // --- CORREÇÃO DO ERRO ---
    // Adicionado EntityGraph para trazer 'itens' e 'itens.produto'
    @EntityGraph(attributePaths = { "itens", "itens.produto", "itens.produto.depositante", "localizacaoAtual" })
    List<Lpn> findBySolicitacaoEntradaIdAndStatus(Long solicitacaoEntradaId, StatusLpn status);

    @EntityGraph(attributePaths = { "itens", "itens.produto", "itens.produto.depositante", "localizacaoAtual" })
    List<Lpn> findBySolicitacaoEntradaIdAndLocalizacaoAtualId(Long solicitacaoEntradaId, Long localizacaoAtualId);

    // Sobrescreve o findById padrão para garantir que buscas individuais (detalhes)
    // não falhem
    @Override
    @EntityGraph(attributePaths = { "itens", "itens.produto", "itens.produto.depositante", "localizacaoAtual" })
    Optional<Lpn> findById(Long id);
}