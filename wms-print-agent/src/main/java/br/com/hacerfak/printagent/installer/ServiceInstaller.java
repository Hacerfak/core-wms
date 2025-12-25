package br.com.hacerfak.printagent.installer;

import br.com.hacerfak.printagent.service.AgentConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceInstaller {

    private final AgentConfigService configService;
    private static final String SERVICE_NAME = "WmsPrintAgent";
    private static final String DISPLAY_NAME = "WMS Print Agent Hub";

    public void install() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                installWindows();
            } else if (os.contains("nix") || os.contains("nux")) {
                installLinux();
            } else {
                throw new UnsupportedOperationException("SO não suportado para instalação automática: " + os);
            }
        } catch (Exception e) {
            log.error("Erro ao instalar serviço", e);
            throw new RuntimeException("Falha na instalação: " + e.getMessage());
        }
    }

    public void uninstall() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                uninstallWindows();
            } else if (os.contains("nix") || os.contains("nux")) {
                uninstallLinux();
            } else {
                throw new UnsupportedOperationException("SO não suportado para desinstalação: " + os);
            }
        } catch (Exception e) {
            log.error("Erro ao desinstalar serviço", e);
            throw new RuntimeException("Falha na desinstalação: " + e.getMessage());
        }
    }

    // --- WINDOWS ---

    private void installWindows() throws Exception {
        String javaExe = System.getProperty("java.home") + "\\bin\\javaw.exe";
        File jarFile = new ApplicationHome(getClass()).getSource();

        // Monta os argumentos JVM com as configs atuais
        String args = String.format(
                "-Dagent.id=%s -Dagent.cnpj=%s -Dagent.domain=%s -Dagent.key=%s -Dserver.port=%s -jar \"%s\"",
                configService.getAgentId(),
                configService.getCnpjEmpresa(),
                configService.getServidorDominio(),
                configService.getApiKey(),
                configService.getServerPort(),
                jarFile.getAbsolutePath());

        String binPath = "\"" + javaExe + "\" " + args;

        log.info("Instalando serviço Windows...");
        // 1. Cria
        runCommand("sc", "create", SERVICE_NAME, "binPath=", binPath, "start=", "auto", "DisplayName=", DISPLAY_NAME);
        // 2. Descrição
        runCommand("sc", "description", SERVICE_NAME, "Agente de Impressão e Conexão WMS");
        // 3. Inicia
        runCommand("sc", "start", SERVICE_NAME);

        log.info("Serviço Windows instalado e iniciado.");
    }

    private void uninstallWindows() throws Exception {
        log.info("Parando serviço Windows...");
        runCommand("sc", "stop", SERVICE_NAME);

        log.info("Removendo serviço Windows...");
        runCommand("sc", "delete", SERVICE_NAME);

        log.info("Serviço Windows removido.");
    }

    // --- LINUX (Systemd) ---

    private void installLinux() throws Exception {
        File jarFile = new ApplicationHome(getClass()).getSource();
        String javaPath = ProcessHandle.current().info().command().orElse("/usr/bin/java");

        String jvmArgs = String.format(
                "-Dagent.id=%s -Dagent.cnpj=%s -Dagent.domain=%s -Dagent.key=%s -Dserver.port=%s",
                configService.getAgentId(),
                configService.getCnpjEmpresa(),
                configService.getServidorDominio(),
                configService.getApiKey(),
                configService.getServerPort());

        String serviceContent = """
                [Unit]
                Description=%s
                After=network.target

                [Service]
                User=root
                Type=simple
                ExecStart=%s %s -jar %s
                Restart=always
                RestartSec=10

                [Install]
                WantedBy=multi-user.target
                """.formatted(DISPLAY_NAME, javaPath, jvmArgs, jarFile.getAbsolutePath());

        File serviceFile = new File("/etc/systemd/system/" + SERVICE_NAME + ".service");
        try (FileWriter writer = new FileWriter(serviceFile)) {
            writer.write(serviceContent);
        }

        runCommand("systemctl", "daemon-reload");
        runCommand("systemctl", "enable", SERVICE_NAME);
        runCommand("systemctl", "start", SERVICE_NAME);
        log.info("Serviço Linux instalado.");
    }

    private void uninstallLinux() throws Exception {
        log.info("Parando serviço Linux...");
        runCommand("systemctl", "stop", SERVICE_NAME);
        runCommand("systemctl", "disable", SERVICE_NAME);

        Files.deleteIfExists(Path.of("/etc/systemd/system/" + SERVICE_NAME + ".service"));

        runCommand("systemctl", "daemon-reload");
        log.info("Serviço Linux removido.");
    }

    private void runCommand(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process p = pb.start();
        // Não validamos exitCode estritamente no uninstall pois o serviço pode já não
        // existir
        p.waitFor();
    }
}