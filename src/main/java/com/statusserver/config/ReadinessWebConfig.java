package com.statusserver.config;

import com.statusserver.status.sync.BootstrapState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Blockiert öffentliche API-Zugriffe, solange die Node noch bootstrapped.
 */
@Configuration
@RequiredArgsConstructor
public class ReadinessWebConfig implements WebMvcConfigurer {
    private final BootstrapState bootstrapState;

    /**
     * Registriert einen Interceptor für die öffentlichen Status-Endpunkte.
     *
     * @param registry Interceptor-Registry von Spring MVC
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new BootstrapReadinessInterceptor())
                .addPathPatterns("/api/status/**");
    }

    /**
     * Prüft vor jedem Status-Request, ob der Bootstrap-Sync abgeschlossen ist.
     */
    private class BootstrapReadinessInterceptor implements HandlerInterceptor {
        /**
         * Lässt Requests nur zu, wenn die Node als bereit markiert wurde.
         *
         * @param request eingehender HTTP-Request
         * @param response ausgehende HTTP-Response
         * @param handler ausgewählter Handler
         * @return {@code true}, wenn der Request weiterverarbeitet werden darf
         * @throws Exception falls das Schreiben der Fehlerantwort fehlschlägt
         */
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            if (bootstrapState.isReady()) {
                return true;
            }

            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(), "Node is bootstrapping");
            return false;
        }
    }

}
