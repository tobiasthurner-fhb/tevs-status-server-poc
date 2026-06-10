package com.statusserver.config;

import com.statusserver.status.messaging.StatusChannels;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Konfiguriert STOMP/WebSocket für Live-Updates an Clients.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final AppProperties appProperties;

    /**
     * Aktiviert den einfachen Broker und definiert die Ziel-Präfixe.
     *
     * @param config Message-Broker-Konfiguration
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(StatusChannels.WS_TOPIC_PREFIX);
        config.setApplicationDestinationPrefixes(StatusChannels.WS_APP_PREFIX);
    }

    /**
     * Registriert den WebSocket-Endpunkt für STOMP-Clients.
     *
     * @param registry Registry für STOMP-Endpunkte
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(StatusChannels.WS_ENDPOINT)
                .setAllowedOrigins(appProperties.getAllowedOrigins().toArray(String[]::new));
    }
}
