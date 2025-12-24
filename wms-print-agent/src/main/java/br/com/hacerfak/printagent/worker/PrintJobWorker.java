package br.com.hacerfak.printagent.worker;

import br.com.hacerfak.printagent.dto.PrintJobDTO;
import br.com.hacerfak.printagent.service.AgentLogStore;
import br.com.hacerfak.printagent.service.NetworkPrinterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@RequiredArgsConstructor
public class PrintJobWorker {

    private final RestTemplate restTemplate;
    private final NetworkPrinterService printerService;
    private final AgentLogStore logStore; // <--- Injeção

    @Value("${agent.backend-url}")
    private String backendUrl;

    @Value("${agent.api-key}")
    private String apiKey;

    @Value("${agent.id}")
    private String agentId;

    public void processarFila() {
        try {
            // ... (Código de Long Polling que fizemos antes) ...
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Exemplo usando Long Polling (ou GET simples dependendo do que implementou)
            ResponseEntity<PrintJobDTO[]> response = restTemplate.exchange(
                    backendUrl + "/poll?agentId=" + agentId,
                    HttpMethod.GET,
                    entity,
                    PrintJobDTO[].class);

            PrintJobDTO[] jobsArray = response.getBody();

            if (jobsArray != null && jobsArray.length > 0) {
                logStore.addLog("Recebidos " + jobsArray.length + " jobs.", false); // Log GUI
                for (PrintJobDTO job : jobsArray) {
                    executarJob(job);
                }
            }
        } catch (Exception e) {
            log.warn("Erro conexão: {}", e.getMessage());
            // Opcional: Não poluir a GUI com erros de conexão repetitivos, ou logar apenas
            // 1x
        }
    }

    private void executarJob(PrintJobDTO job) {
        try {
            log.info("Processando Job #{}", job.getId());

            if ("REDE".equalsIgnoreCase(job.getTipoConexao())) {
                printerService.imprimirViaSocket(job.getIp(), job.getPorta(), job.getZpl());
            } else if ("COMPARTILHAMENTO".equalsIgnoreCase(job.getTipoConexao()) ||
                    "USB_LOCAL".equalsIgnoreCase(job.getTipoConexao())) {
                printerService.imprimirViaCompartilhamento(job.getCaminhoCompartilhamento(), job.getZpl());
            } else {
                throw new IllegalArgumentException(
                        "Tipo de conexão não suportado pelo Agente: " + job.getTipoConexao());
            }

            // 2. Reportar SUCESSO ao Backend para tirar da fila
            // POST http://localhost:8080/api/impressao/fila/{id}/concluir
            restTemplate.postForLocation(backendUrl + "/" + job.getId() + "/concluir", null);
            log.info("Job #{} impresso e confirmado.", job.getId());

            // LOG GUI
            logStore.addLog("Job #" + job.getId() + " IMPRESSO com sucesso na " + job.getTipoConexao(), false);

        } catch (Exception e) {
            log.error("Falha ao imprimir Job #{}", job.getId(), e);

            // LOG GUI ERRO
            logStore.addLog("ERRO Job #" + job.getId() + ": " + e.getMessage(), true);

            // 3. Reportar ERRO ao Backend
            try {
                restTemplate.postForLocation(backendUrl + "/" + job.getId() + "/erro", e.getMessage());
            } catch (Exception reportEx) {
                log.error("Falha ao reportar erro ao backend", reportEx);
            }
        }
    }
}