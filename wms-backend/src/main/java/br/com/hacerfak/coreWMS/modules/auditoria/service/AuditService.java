package br.com.hacerfak.coreWMS.modules.auditoria.service;

import br.com.hacerfak.coreWMS.core.config.MessagingConfig;
import br.com.hacerfak.coreWMS.core.util.DiffUtils;
import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final RabbitTemplate rabbitTemplate;
    private final DiffUtils diffUtils;

    /**
     * AUDITORIA AUTOMÁTICA (Smart Diff)
     * Agora recebe o contexto (tenant, user, ip) explicitamente para funcionar
     * com @Async
     */
    @Async
    public void registrarAuditoria(String evento, Object entidade, String entidadeId, Object estadoAntigo,
            Object estadoNovo, String tenantId, String usuario, String ip, String userAgent) {
        try {
            // 1. Gera o Diff
            String diff = diffUtils.gerarDiff(estadoAntigo, estadoNovo);

            // Se for UPDATE e nada mudou, ignora
            if ("UPDATE".equals(evento) && (diff == null || diff.equals("{}"))) {
                return;
            }

            String nomeEntidade = entidade != null ? entidade.getClass().getSimpleName() : "Desconhecido";

            // 2. Monta e envia
            enviarLog(evento, nomeEntidade, entidadeId, diff, tenantId, usuario, ip, userAgent);

        } catch (Exception e) {
            log.error("Falha ao salvar auditoria automática", e);
        }
    }

    /**
     * AUDITORIA MANUAL
     */
    @Async
    public void registrarLog(String evento, String entidade, String entidadeId, String mensagem,
            String tenantId, String usuario, String ip, String userAgent) {
        try {
            enviarLog(evento, entidade, entidadeId, mensagem, tenantId, usuario, ip, userAgent);
        } catch (Exception e) {
            log.error("Falha ao salvar log manual", e);
        }
    }

    // Método privado unificado para envio
    private void enviarLog(String evento, String entidade, String entidadeId, String dados,
            String tenantId, String usuario, String ip, String userAgent) {

        // Proteção contra nulos (caso venha vazio do chamador)
        if (tenantId == null)
            tenantId = "public";
        if (usuario == null)
            usuario = "SISTEMA";

        AuditLog logEntry = AuditLog.builder()
                .tenantId(tenantId)
                .evento(evento)
                .entidade(entidade)
                .entidadeId(entidadeId)
                .usuario(usuario)
                .ipOrigem(ip)
                .userAgent(userAgent)
                .dados(dados)
                .build();

        // Envia para o RabbitMQ
        rabbitTemplate.convertAndSend(MessagingConfig.QUEUE_AUDITORIA, logEntry);
    }
}