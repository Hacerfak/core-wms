package br.com.hacerfak.coreWMS.modules.impressao.repository;

import br.com.hacerfak.coreWMS.modules.impressao.domain.AgenteImpressao;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgenteImpressaoRepository extends JpaRepository<AgenteImpressao, Long> {

    /// --- OTIMIZAÇÃO DE PERFORMANCE ---
    // Cacheia o resultado. Se o agente chamar 1000x, 999x serão respondidas pela
    /// memória RAM (Redis)
    // O nome "agentes-key" é a chave do cache
    @Cacheable(value = "agentes-key", key = "#apiKey", unless = "#result == null")
    Optional<AgenteImpressao> findByApiKeyAndAtivoTrue(String apiKey);

    boolean existsByNome(String nome);
}