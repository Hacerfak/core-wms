package br.com.hacerfak.coreWMS.modules.impressao.repository;

import br.com.hacerfak.coreWMS.modules.impressao.domain.AgenteImpressao;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgenteImpressaoRepository extends JpaRepository<AgenteImpressao, Long> {

    // Cacheia a busca por API Key para não ir no banco a cada 3 segundos
    // O nome do cache "agentes-key" deve ser limpo se o admin revogar a chave
    @Cacheable(value = "agentes-key", key = "#apiKey", unless = "#result == null")
    Optional<AgenteImpressao> findByApiKeyAndAtivoTrue(String apiKey);

    boolean existsByNome(String nome);
}