package br.com.hacerfak.printagent.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Getter
public class AgentStatusService {

    private boolean online = false;
    private LocalDateTime lastCheck;
    private String lastMessage = "Iniciando...";

    public void updateStatus(boolean isOnline, String message) {
        this.online = isOnline;
        this.lastMessage = message;
        this.lastCheck = LocalDateTime.now();
    }

    public String getLastCheckFormatted() {
        if (lastCheck == null)
            return "-";
        return lastCheck.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}