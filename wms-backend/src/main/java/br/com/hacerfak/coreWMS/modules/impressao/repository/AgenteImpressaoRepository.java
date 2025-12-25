package br.com.hacerfak.coreWMS.modules.impressao.repository;

import br.com.hacerfak.coreWMS.modules.impressao.domain.AgenteImpressao;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
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

    // --- CORREÇÃO: Update direto para não gerar Auditoria ---
    @Modifying(clearAutomatically = true) // Limpa o cache para evitar dados obsoletos na sessão atual
    @Query("UPDATE AgenteImpressao a SET a.ultimoHeartbeat = :data, a.versaoAgente = :versao WHERE a.id = :id")
    void registrarHeartbeatSemAuditoria(@Param("id") Long id, @Param("data") LocalDateTime data,
            @Param("versao") String versao);
}