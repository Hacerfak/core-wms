package br.com.hacerfak.coreWMS.core.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
@Slf4j // Adicionado Lombok SLF4J
public class MongoConfig extends AbstractMongoClientConfiguration {

    // Injeta a URI definida no Docker (JAVA_TOOL_OPTIONS ou environment)
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        return "wms_audit_global";
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        log.info("Iniciando conexão MongoDB com URI dedicada para Auditoria: {}", mongoUri);

        ConnectionString connectionString = new ConnectionString(mongoUri);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(settings);
    }
}