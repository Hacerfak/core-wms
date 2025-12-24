package br.com.hacerfak.coreWMS.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter; // Usamos a classe comprovada
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    public static final String QUEUE_AUDITORIA = "wms.auditoria.queue";
    public static final String QUEUE_FATURAMENTO = "wms.faturamento.queue";

    @Bean
    public Queue auditoriaQueue() {
        return new Queue(QUEUE_AUDITORIA, true);
    }

    @Bean
    public Queue faturamentoQueue() {
        return new Queue(QUEUE_FATURAMENTO, true);
    }

    /**
     * Configura o conversor JSON.
     * Usamos @SuppressWarnings("deprecation") para ignorar o aviso do Spring Boot 4
     * e usar a implementação estável que aceita o ObjectMapper corretamente.
     * ObjectMapper objectMapper
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}