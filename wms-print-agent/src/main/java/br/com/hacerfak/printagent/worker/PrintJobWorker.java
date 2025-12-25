package br.com.hacerfak.printagent.worker;

import br.com.hacerfak.printagent.dto.PrintJobDTO;
import br.com.hacerfak.printagent.service.AgentConfigService; // <--- NOVO
import br.com.hacerfak.printagent.service.AgentLogStore;
import br.com.hacerfak.printagent.service.AgentStatusService;
import br.com.hacerfak.printagent.service.NetworkPrinterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Component
@Slf4j
@RequiredArgsConstructor
public class PrintJobWorker {

    private final RestTemplate restTemplate;
    private final NetworkPrinterService printerService;
    private final AgentLogStore logStore;
    private final AgentStatusService statusService;
    private final AgentConfigService configService;

    @Value("${info.app.version:dev}")
    private String agentVersion;

    @Scheduled(fixedDelayString = "${agent.poll-interval:5000}", initialDelay = 3000)
    public void processarFila() {
        // Validação se está configurado
        if (!configService.isConfigurado()) {
            statusService.updateStatus(false, "Aguardando Configuração...");
            return;
        }

        try {
            // Pega dados dinâmicos do serviço
            String urlCompleta = configService.getBackendUrl() + "/poll?agentId=" + configService.getAgentId();

            HttpEntity<String> entity = new HttpEntity<>(getHeaders());

            ResponseEntity<PrintJobDTO[]> response = restTemplate.exchange(
                    urlCompleta,
                    HttpMethod.GET,
                    entity,
                    PrintJobDTO[].class);

            statusService.updateStatus(true, "Conectado: " + configService.getTenantId());

            PrintJobDTO[] jobsArray = response.getBody();
            if (jobsArray != null && jobsArray.length > 0) {
                logStore.addLog("Recebidos " + jobsArray.length + " jobs.", false);
                for (PrintJobDTO job : jobsArray) {
                    executarJob(job);
                }
            }
        } catch (ResourceAccessException e) {
            statusService.updateStatus(false, "Falha conexão: " + configService.getServidorDominio());
        } catch (Exception e) {
            String msg = "Erro: " + e.getMessage();
            log.error(msg);
            statusService.updateStatus(false, msg);
            logStore.addLog(msg, true);
        }
    }

    private void executarJob(PrintJobDTO job) {
        try {
            log.info("Job #{}", job.getId());
            if ("REDE".equalsIgnoreCase(job.getTipoConexao())) {
                printerService.imprimirViaSocket(job.getIp(), job.getPorta(), job.getZpl());
            } else {
                printerService.imprimirViaCompartilhamento(job.getCaminhoCompartilhamento(), job.getZpl());
            }

            // --- CONFIRMAR ---
            // Agora enviamos os headers também na confirmação
            String confirmUrl = configService.getBackendUrl() + "/" + job.getId() + "/concluir";
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

            // Usamos 'exchange' ou 'postForLocation' passando a entity com headers
            restTemplate.postForLocation(confirmUrl, entity);

            logStore.addLog("Job #" + job.getId() + " OK", false);
        } catch (Exception e) {
            log.error("Falha job #{}", job.getId(), e);
            logStore.addLog("ERRO Job #" + job.getId() + ": " + e.getMessage(), true);
            try {
                // Enviamos headers também no endpoint de erro
                String erroUrl = configService.getBackendUrl() + "/" + job.getId() + "/erro";

                // O corpo da requisição é a mensagem de erro, e os headers vão junto
                HttpEntity<String> entity = new HttpEntity<>(e.getMessage(), getHeaders());
                restTemplate.postForLocation(erroUrl, entity);
            } catch (Exception ignored) {
            }
        }
    }

    // Método auxiliar para garantir que TODAS as requisições tenham os dados do
    // Tenant
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Agent-Key", configService.getApiKey());
        headers.set("X-Tenant-ID", configService.getTenantId());
        headers.set("X-Agent-Version", agentVersion);
        return headers;
    }
}