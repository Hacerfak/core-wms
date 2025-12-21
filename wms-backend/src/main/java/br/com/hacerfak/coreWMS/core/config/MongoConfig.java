package br.com.hacerfak.coreWMS.core.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
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
        // LOG PARA PROVA REAL (Vai aparecer no console do Docker)
        System.out.println("=============================================");
        System.out.println(">>> FORCANDO CONEXAO MONGO MANUALMENTE <<<");
        System.out.println(">>> URI USADA: " + mongoUri);
        System.out.println("=============================================");

        ConnectionString connectionString = new ConnectionString(mongoUri);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        return MongoClients.create(settings);
    }
}