package br.com.hacerfak.coreWMS;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import br.com.hacerfak.coreWMS.core.config.UserAuditorAware;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync // <--- Habilita processamento em segundo plano para os logs
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class CoreWmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreWmsApplication.class, args);
	}

	@Bean
	public AuditorAware<String> auditorAware() {
		return new UserAuditorAware();
	}

	@Bean
	public CommandLineRunner debugMongoConfig(
			@Value("${spring.data.mongodb.host:NAO_DEFINIDO}") String host,
			@Value("${spring.data.mongodb.uri:NAO_DEFINIDO}") String uri) {
		return args -> {
			System.out.println("=========================================");
			System.out.println(">>> DEBUG MONGO CONFIG <<<");
			System.out.println("HOST VISTO PELO SPRING: " + host);
			System.out.println("URI VISTA PELO SPRING: " + uri);
			System.out.println("=========================================");
		};
	}

}
