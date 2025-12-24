package br.com.hacerfak.coreWMS.modules.auditoria.job;

import br.com.hacerfak.coreWMS.modules.auditoria.repository.AuditLogRepository;
import br.com.hacerfak.coreWMS.modules.sistema.domain.SistemaConfig;
import br.com.hacerfak.coreWMS.modules.sistema.repository.SistemaConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogCleanupJob {

    private final AuditLogRepository auditLogRepository;
    private final SistemaConfigRepository sistemaConfigRepository;

    private static final String CONFIG_KEY = "AUDIT_RETENTION_DAYS";
    private static final int DEFAULT_RETENTION_DAYS = 90;

    // Roda todos os dias às 03:00 da manhã
    @Scheduled(cron = "0 0 3 * * ?")
    public void executarLimpeza() {
        log.info(">>> Iniciando Job de Limpeza de Auditoria...");

        int diasRetencao = getDiasRetencaoConfigurados();

        LocalDateTime dataCorte = LocalDateTime.now().minusDays(diasRetencao);

        try {
            log.info("Apagando logs anteriores a: {}", dataCorte);
            auditLogRepository.deleteByDataHoraBefore(dataCorte);
            log.info(">>> Limpeza de Auditoria concluída com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao executar limpeza de auditoria", e);
        }
    }

    private int getDiasRetencaoConfigurados() {
        try {
            Optional<SistemaConfig> config = sistemaConfigRepository.findById(CONFIG_KEY);
            if (config.isPresent()) {
                return Integer.parseInt(config.get().getValor());
            }
        } catch (NumberFormatException e) {
            log.warn("Valor inválido para {}. Usando padrão.", CONFIG_KEY);
        }
        return DEFAULT_RETENTION_DAYS;
    }
}