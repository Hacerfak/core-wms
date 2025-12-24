package br.com.hacerfak.coreWMS.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 1. Suporte a Datas do Java 8+ (LocalDateTime, etc)
        mapper.registerModule(new JavaTimeModule());

        // 2. Desativa escrita de datas como Timestamp numérico (usa ISO-8601:
        // "2025-12-24T10:00:00")
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3. Ignora propriedades desconhecidas no JSON (evita quebras se o front mandar
        // campos extras)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 4. Suporte ao Hibernate (Lazy Loading)
        // Se você estiver usando o 'jackson-datatype-hibernate7' no pom.xml:
        // Caso a classe Hibernate7Module não exista ainda no seu classpath,
        // use Hibernate6Module ou verifique a importação correta da lib
        // 'jackson-datatype-hibernate7'
        try {
            // O Spring Boot 4 com Hibernate 7 deve usar o módulo compatível
            // Se a classe exata não for encontrada, o try/catch evita que o app quebre,
            // mas o ideal é ajustar o import conforme sua dependência do Maven.
            com.fasterxml.jackson.datatype.hibernate7.Hibernate7Module hibernateModule = new com.fasterxml.jackson.datatype.hibernate7.Hibernate7Module();

            // Opcional: Força o carregamento de lazy objects (Cuidado com performance)
            // hibernateModule.configure(com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module.Feature.FORCE_LAZY_LOADING,
            // true);

            mapper.registerModule(hibernateModule);
        } catch (NoClassDefFoundError | Exception e) {
            System.out.println(">>> AVISO: Módulo Jackson-Hibernate não carregado: " + e.getMessage());
        }

        return mapper;
    }
}