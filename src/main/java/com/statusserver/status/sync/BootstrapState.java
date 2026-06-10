package com.statusserver.status.sync;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-sicherer Bereitschaftszustand der Node während des Bootstraps.
 */
@Component
public class BootstrapState {
    private final AtomicBoolean ready = new AtomicBoolean(false);

    /**
     * Gibt an, ob die Node öffentliche Client-Requests beantworten darf.
     *
     * @return {@code true}, wenn der Bootstrap abgeschlossen ist
     */
    public boolean isReady() {
        return ready.get();
    }

    /**
     * Markiert die Node nach erfolgreichem Bootstrap-Sync, nach erlaubtem Timeout
     * oder ohne konfigurierte Peers als bereit.
     */
    public void markReady() {
        ready.set(true);
    }
}
