package com.statusserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Einstiegspunkt der Spring-Boot-Anwendung für den verteilten Statusserver.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class StatusServerApplication {
    /**
     * Startet die Anwendung mit der übergebenen Kommandozeilenkonfiguration.
     *
     * @param args Kommandozeilenargumente für Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(StatusServerApplication.class, args);
    }
}
