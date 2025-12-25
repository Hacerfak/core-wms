package br.com.hacerfak.printagent;

import br.com.hacerfak.printagent.installer.ServiceInstaller;
import br.com.hacerfak.printagent.service.AgentConfigService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class WmsPrintAgentApplication {

	public static void main(String[] args) {
		// 1. Carrega o .env ANTES de qualquer decisão
		loadEnvVariables();

		if (args.length > 0) {
			handleCliCommands(args);
		} else {
			// Modo Standalone / GUI
			// Define headless=false para permitir GUI se o SO suportar
			System.setProperty("java.awt.headless", "false");
			SpringApplication.run(WmsPrintAgentApplication.class, args);
		}
	}

	/**
	 * Procura um arquivo .env na mesma pasta do executável/jar e carrega
	 * as variáveis para o System Properties do Java.
	 */
	private static void loadEnvVariables() {
		try {
			// Descobre onde o JAR/EXE está rodando
			ApplicationHome home = new ApplicationHome(WmsPrintAgentApplication.class);
			File jarDir = home.getDir();
			File envFile = new File(jarDir, ".env");

			if (envFile.exists()) {
				System.out.println(">>> [INIT] Carregando arquivo .env encontrado em: " + envFile.getAbsolutePath());
				List<String> lines = Files.readAllLines(envFile.toPath());

				for (String line : lines) {
					line = line.trim();
					// Ignora comentários e linhas vazias
					if (line.isEmpty() || line.startsWith("#"))
						continue;

					String[] parts = line.split("=", 2);
					if (parts.length == 2) {
						String key = parts[0].trim();
						String value = parts[1].trim();

						// Remove aspas se houver (ex: API_KEY="123")
						if (value.startsWith("\"") && value.endsWith("\"")) {
							value = value.substring(1, value.length() - 1);
						} else if (value.startsWith("'") && value.endsWith("'")) {
							value = value.substring(1, value.length() - 1);
						}

						// Injeta no sistema como se fosse -Dkey=value
						System.setProperty(key, value);
					}
				}
			} else {
				// System.out.println(">>> [INIT] Usando configurações padrão...");
			}
		} catch (Exception e) {
			System.err.println("!!! Erro crítico ao ler arquivo .env: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void handleCliCommands(String[] args) {
		// --- CORREÇÃO AQUI ---
		// Como o Spring não iniciou, instanciamos as dependências manualmente
		AgentConfigService configService = new AgentConfigService();
		configService.init(); // Força o carregamento do arquivo agent.properties

		// Agora passamos o configService para o instalador
		ServiceInstaller installer = new ServiceInstaller(configService);
		// ---------------------

		String command = args[0];

		if ("--install".equalsIgnoreCase(command)) {
			System.out.println(">>> Instalando Serviço...");
			installer.install();
			System.exit(0);
		} else if ("--uninstall".equalsIgnoreCase(command)) {
			System.out.println(">>> Removendo Serviço...");
			installer.uninstall();
			System.exit(0);
		}

		// Se passar argumentos desconhecidos, roda a aplicação normal
		SpringApplication.run(WmsPrintAgentApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder
				.connectTimeout(Duration.ofSeconds(10))
				.readTimeout(Duration.ofSeconds(40))
				.build();
	}
}