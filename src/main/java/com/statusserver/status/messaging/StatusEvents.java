package com.statusserver.status.messaging;

/**
 * Zentrale Namen für Status-Ereignistypen und Event-Präfixe.
 */
public final class StatusEvents {
    public static final String UPSERT = "UPSERT";
    public static final String DELETE = "DELETE";
    public static final String DELETE_PREFIX = "DELETED:";

    /**
     * Verhindert Instanziierung dieser Konstantenklasse.
     */
    private StatusEvents() {
    }
}
