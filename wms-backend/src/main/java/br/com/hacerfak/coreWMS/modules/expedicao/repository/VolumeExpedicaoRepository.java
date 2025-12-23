package br.com.hacerfak.coreWMS.modules.expedicao.repository;

import br.com.hacerfak.coreWMS.modules.expedicao.domain.VolumeExpedicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VolumeExpedicaoRepository extends JpaRepository<VolumeExpedicao, Long> {

    // Busca por etiqueta de rastreio (para conferência)
    Optional<VolumeExpedicao> findByCodigoRastreio(String codigoRastreio);

    // Verificar se já existe volume aberto para o pedido
    boolean existsBySolicitacaoIdAndFechadoFalse(Long solicitacaoId);
}