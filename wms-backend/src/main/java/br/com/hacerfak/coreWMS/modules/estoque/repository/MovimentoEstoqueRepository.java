package br.com.hacerfak.coreWMS.modules.estoque.repository;

import br.com.hacerfak.coreWMS.modules.estoque.domain.MovimentoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentoEstoqueRepository extends JpaRepository<MovimentoEstoque, Long> {

    /**
     * Busca o "Kardex" do produto: Todo o histórico de movimentação.
     * O OrderByDataMovimentoDesc garante que o último movimento apareça primeiro.
     */
    List<MovimentoEstoque> findByProdutoIdOrderByDataMovimentoDesc(Long produtoId);

    /**
     * Rastreabilidade por Lote:
     * Útil para casos de Recall. Mostra onde esse lote entrou e para onde saiu.
     */
    List<MovimentoEstoque> findByLote(String lote);

    /**
     * Rastreabilidade por Serial:
     * Descobre exatamente quando aquele item único entrou ou saiu.
     */
    List<MovimentoEstoque> findByNumeroSerie(String numeroSerie);

    /**
     * Auditoria por Usuário:
     * Descobre tudo que um operador específico fez.
     */
    List<MovimentoEstoque> findByUsuarioResponsavel(String usuarioResponsavel);
}
