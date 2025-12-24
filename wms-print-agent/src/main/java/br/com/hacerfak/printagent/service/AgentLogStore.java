package br.com.hacerfak.printagent.service;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Component
public class AgentLogStore {

    private static final int MAX_LOGS = 50; // Guarda as últimas 50 linhas
    private final ConcurrentLinkedDeque<String> logBuffer = new ConcurrentLinkedDeque<>();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void addLog(String message, boolean isError) {
        String timestamp = LocalDateTime.now().format(dtf);
        String prefix = isError ? "[ERRO] " : "[INFO] ";
        String entry = String.format("%s %s%s", timestamp, prefix, message);

        logBuffer.addFirst(entry); // Adiciona no topo

        // Mantém o tamanho fixo
        if (logBuffer.size() > MAX_LOGS) {
            logBuffer.removeLast();
        }
    }

    public String getLogsAsString() {
        return logBuffer.stream().collect(Collectors.joining("\n"));
    }

    public void clear() {
        logBuffer.clear();
    }
}