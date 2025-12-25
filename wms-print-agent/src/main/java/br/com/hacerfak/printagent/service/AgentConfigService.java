package br.com.hacerfak.printagent.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Properties;

@Service
@Slf4j
public class AgentConfigService {

    private static final String CONFIG_FILE = "agent.properties";

    @Getter
    @Setter
    private String agentId = "AGENTE_NOVO";
    @Getter
    @Setter
    private String cnpjEmpresa = "";
    @Getter
    @Setter
    private String servidorDominio = "localhost:8080";
    @Getter
    @Setter
    private String apiKey = "";
    @Getter
    @Setter
    private String serverPort = "8090";

    // Caminho calculado (não salvo)
    public String getTenantId() {
        if (cnpjEmpresa == null || cnpjEmpresa.isBlank())
            return "wms_master";
        String cnpjLimpo = cnpjEmpresa.replaceAll("\\D", "");
        return "tenant_" + cnpjLimpo;
    }

    public String getBackendUrl() {
        String dominio = servidorDominio.trim();
        if (!dominio.startsWith("http")) {
            dominio = "http://" + dominio;
        }
        // Remove barra final se tiver
        if (dominio.endsWith("/")) {
            dominio = dominio.substring(0, dominio.length() - 1);
        }
        return dominio + "/api/impressao/fila";
    }

    @PostConstruct
    public void init() {
        carregarConfiguracoes();
    }

    public void salvarConfiguracoes(String id, String cnpj, String dominio, String key) {
        this.agentId = id;
        this.cnpjEmpresa = cnpj;
        this.servidorDominio = dominio;
        this.apiKey = key;

        Properties props = new Properties();
        props.setProperty("agent.id", agentId);
        props.setProperty("agent.cnpj", cnpjEmpresa);
        props.setProperty("agent.domain", servidorDominio);
        props.setProperty("agent.key", apiKey);
        props.setProperty("server.port", serverPort); // Mantém a porta

        try {
            File file = getConfigFile();
            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "Configuracoes WMS Print Agent");
            }
            log.info("Configurações salvas em: {}", file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Erro ao salvar configurações", e);
        }
    }

    private void carregarConfiguracoes() {
        try {
            File file = getConfigFile();
            if (file.exists()) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
                this.agentId = props.getProperty("agent.id", agentId);
                this.cnpjEmpresa = props.getProperty("agent.cnpj", cnpjEmpresa);
                this.servidorDominio = props.getProperty("agent.domain", servidorDominio);
                this.apiKey = props.getProperty("agent.key", apiKey);
                this.serverPort = props.getProperty("server.port", serverPort);
                log.info("Configurações carregadas de: {}", file.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Erro ao carregar configurações", e);
        }
    }

    private File getConfigFile() {
        ApplicationHome home = new ApplicationHome(getClass());
        return new File(home.getDir(), CONFIG_FILE);
    }

    public boolean isConfigurado() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("SEGREDO_PADRAO");
    }
}