package com.statusserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Konfiguriert Web-MVC-Einstellungen für die REST-Schnittstelle.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /**
     * Erlaubt CORS-Zugriffe auf die REST-API für externe Clients.
     *
     * @param registry CORS-Registry von Spring MVC
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
