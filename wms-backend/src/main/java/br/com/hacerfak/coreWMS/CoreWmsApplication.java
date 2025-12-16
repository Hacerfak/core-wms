package br.com.hacerfak.coreWMS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing // Habilita auditoria JPA (ex: @CreatedDate, @LastModifiedDate)
public class CoreWmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreWmsApplication.class, args);
	}

}
