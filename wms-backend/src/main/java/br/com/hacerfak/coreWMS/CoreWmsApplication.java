package br.com.hacerfak.coreWMS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import br.com.hacerfak.coreWMS.core.config.UserAuditorAware;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync // <--- Habilita processamento em segundo plano para os logs
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)

// --- 1. CONFIGURAÇÃO DO JPA (POSTGRES) ---
// Varre o projeto todo, MAS EXCLUI a pasta do Mongo para não confundir
@EnableJpaRepositories(basePackages = "br.com.hacerfak.coreWMS", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "br\\.com\\.hacerfak\\.coreWMS\\.modules\\.auditoria\\.repository\\..*"))

// --- 2. CONFIGURAÇÃO DO MONGO ---
// Varre APENAS a pasta específica de auditoria
@EnableMongoRepositories(basePackages = "br.com.hacerfak.coreWMS.modules.auditoria.repository")
public class CoreWmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreWmsApplication.class, args);
	}

	@Bean
	public AuditorAware<String> auditorAware() {
		return new UserAuditorAware();
	}
}
