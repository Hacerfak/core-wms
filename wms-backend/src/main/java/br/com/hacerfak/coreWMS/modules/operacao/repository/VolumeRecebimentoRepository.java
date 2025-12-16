package br.com.hacerfak.coreWMS.modules.operacao.repository;

import br.com.hacerfak.coreWMS.modules.operacao.domain.VolumeRecebimento;
import br.com.hacerfak.coreWMS.modules.operacao.dto.VolumeResumoDTO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional; // <--- Importante

@Repository
public interface VolumeRecebimentoRepository extends JpaRepository<VolumeRecebimento, Long> {

    // --- CORREÇÃO: Adicionado o método que faltava ---
    Optional<VolumeRecebimento> findByLpn(String lpn);

    List<VolumeRecebimento> findByRecebimentoId(Long recebimentoId);

    @Query("SELECT COALESCE(SUM(v.quantidadeOriginal), 0) FROM VolumeRecebimento v WHERE v.recebimento.id = :recebimentoId AND v.produto.sku = :sku")
    BigDecimal somarQuantidadePorSku(@Param("recebimentoId") Long recebimentoId, @Param("sku") String sku);

    boolean existsByLpn(String lpn);

    void deleteByRecebimentoId(Long recebimentoId);

    // NOVO: Busca volumes de um produto específico dentro da nota
    // --- CORREÇÃO: Busca Otimizada com DTO ---
    @Query("""
               SELECT new br.com.hacerfak.coreWMS.modules.operacao.dto.VolumeResumoDTO(
                   v.id,
                   v.lpn,
                   v.quantidadeOriginal
               )
               FROM VolumeRecebimento v
               WHERE v.recebimento.id = :recebimentoId
               AND v.produto.id = :produtoId
               ORDER BY v.id DESC
            """)
    List<VolumeResumoDTO> findResumoByRecebimentoAndProduto(
            @Param("recebimentoId") Long recebimentoId,
            @Param("produtoId") Long produtoId);
}