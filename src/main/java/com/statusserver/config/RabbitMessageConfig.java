package com.statusserver.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguriert die JSON-Serialisierung für RabbitMQ-Nachrichten.
 */
@Configuration
public class RabbitMessageConfig {
    /**
     * Erstellt einen MessageConverter mit Java-Time-Unterstützung.
     *
     * @param objectMapper zentraler Jackson-Mapper der Anwendung
     * @return JSON-MessageConverter für AMQP
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
