package com.statusserver.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Typisierte Anwendungskonfiguration für Node-Identität und Bootstrap-Sync.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String nodeId = "default-node";
    private List<String> peerBaseUrls = new ArrayList<>();
    private Duration bootstrapTimeout = Duration.ofSeconds(30);
    private Duration bootstrapRetryDelay = Duration.ofSeconds(2);
    private boolean bootstrapRequirePeer = true;
    private List<String> allowedOrigins = List.of("http://localhost:4200", "http://127.0.0.1:4200");
}
