package br.com.hacerfak.printagent.installer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ServiceInstaller {

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
                log.error("Sistema operacional não suportado para instalação automática: " + os);
            }
        } catch (Exception e) {
            log.error("Erro fatal ao instalar serviço", e);
        }
    }

    public void uninstall() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                runCommand("sc", "stop", SERVICE_NAME);
                runCommand("sc", "delete", SERVICE_NAME);
                log.info("Serviço Windows removido com sucesso.");
            } else if (os.contains("nix") || os.contains("nux")) {
                runCommand("systemctl", "stop", SERVICE_NAME);
                runCommand("systemctl", "disable", SERVICE_NAME);
                Files.deleteIfExists(Path.of("/etc/systemd/system/" + SERVICE_NAME + ".service"));
                runCommand("systemctl", "daemon-reload");
                log.info("Serviço Linux removido com sucesso.");
            }
        } catch (Exception e) {
            log.error("Erro ao desinstalar serviço", e);
        }
    }

    private void installLinux() throws IOException, InterruptedException {
        log.info("Iniciando instalação no Linux (Systemd)...");

        File jarFile = getJarFile();
        File envFile = new File(jarFile.getParent(), ".env");
        String jarPath = jarFile.getAbsolutePath();
        String javaPath = ProcessHandle.current().info().command().orElse("/usr/bin/java");

        // Verifica se existe .env para adicionar a diretiva EnvironmentFile
        String envDirective = "";
        if (envFile.exists()) {
            envDirective = "EnvironmentFile=" + envFile.getAbsolutePath();
            log.info("Arquivo .env detectado: " + envFile.getAbsolutePath());
        } else {
            log.warn("Arquivo .env não encontrado. Usando variáveis padrão.");
        }

        String serviceContent = """
                [Unit]
                Description=%s
                After=network.target

                [Service]
                User=root
                Type=simple
                ExecStart=%s -jar %s
                Restart=always
                RestartSec=10
                %s

                [Install]
                WantedBy=multi-user.target
                """.formatted(DISPLAY_NAME, javaPath, jarPath, envDirective);

        File serviceFile = new File("/etc/systemd/system/" + SERVICE_NAME + ".service");

        try (FileWriter writer = new FileWriter(serviceFile)) {
            writer.write(serviceContent);
        }

        log.info("Arquivo de serviço criado em: " + serviceFile.getAbsolutePath());

        // Habilita e inicia
        runCommand("systemctl", "daemon-reload");
        runCommand("systemctl", "enable", SERVICE_NAME);
        runCommand("systemctl", "start", SERVICE_NAME);

        log.info("Serviço Linux INSTALADO e INICIADO com sucesso! 🐧");
    }

    private void installWindows() throws IOException, InterruptedException {
        log.info("Iniciando instalação no Windows...");

        File jarFile = getJarFile();
        File envFile = new File(jarFile.getParent(), ".env");
        String jarPath = jarFile.getAbsolutePath();
        String javaExe = System.getProperty("java.home") + "\\bin\\javaw.exe";

        // Monta os argumentos JVM baseados no .env
        StringBuilder jvmArgs = new StringBuilder();
        if (envFile.exists()) {
            log.info("Lendo variáveis do arquivo .env...");
            Map<String, String> envVars = parseEnvFile(envFile);

            // Converte KEY=VALUE em -DKEY=VALUE para o Java
            envVars.forEach((k, v) -> {
                jvmArgs.append(" -D").append(k).append("=\"").append(v).append("\"");
            });
        }

        // binPath = "C:\...\javaw.exe -DAGENT_ID=HUB1 -DAPI_KEY=123 -jar
        // C:\...\app.jar"
        String binPath = "\"" + javaExe + "\"" + jvmArgs.toString() + " -jar \"" + jarPath + "\"";

        log.info("BinPath gerado: " + binPath);

        // 1. Cria o serviço
        int result = runCommand("sc", "create", SERVICE_NAME,
                "binPath=", binPath,
                "start=", "auto",
                "DisplayName=", DISPLAY_NAME);

        if (result == 0 || result == 1073) { // 0=OK, 1073=Já existe
            // 2. Configura descrição
            runCommand("sc", "description", SERVICE_NAME, "Hub de Impressão para CoreWMS");
            // 3. Inicia
            runCommand("sc", "start", SERVICE_NAME);
            log.info("Serviço Windows INSTALADO e INICIADO com sucesso! 🪟");
            log.info("Nota: Se falhou, certifique-se de rodar o CMD/PowerShell como ADMINISTRADOR.");
        } else {
            log.error("Falha ao criar serviço Windows via SC. Código: " + result);
        }
    }

    private File getJarFile() {
        // Pega o caminho real onde o .jar está rodando
        ApplicationHome home = new ApplicationHome(ServiceInstaller.class);
        return home.getSource();
    }

    private int runCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO(); // Mostra a saída no console do usuário
        Process p = pb.start();
        return p.waitFor();
    }

    /**
     * Helper simples para ler arquivo .env (chave=valor)
     */
    private Map<String, String> parseEnvFile(File file) {
        Map<String, String> vars = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                line = line.trim();
                // Ignora comentários e linhas vazias
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    // Remove aspas se houver
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    vars.put(key, value);
                }
            }
        } catch (IOException e) {
            log.error("Erro ao ler arquivo .env", e);
        }
        return vars;
    }
}