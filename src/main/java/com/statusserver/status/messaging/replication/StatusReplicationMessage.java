package com.statusserver.status.messaging.replication;

import com.statusserver.status.domain.StatusMessage;
import com.statusserver.status.messaging.StatusEvents;
import java.time.OffsetDateTime;

/**
 * Nachricht für die asynchrone Replikation von Status-Upserts und Löschungen.
 *
 * @param sourceNodeId Node-ID des Absenders
 * @param eventType Ereignistyp, z. B. UPSERT oder DELETE
 * @param username eindeutiger Benutzername
 * @param statustext textuelle Statusmeldung bei UPSERT
 * @param uhrzeit fachlicher Änderungs- oder Löschzeitpunkt
 * @param latitude geographische Breite bei UPSERT
 * @param longitude geographische Länge bei UPSERT
 */
public record StatusReplicationMessage(
        String sourceNodeId,
        String eventType,
        String username,
        String statustext,
        OffsetDateTime uhrzeit,
        double latitude,
        double longitude
) {
    /**
     * Erstellt eine UPSERT-Replikationsnachricht aus einer Statusmeldung.
     *
     * @param sourceNodeId Node-ID des Absenders
     * @param statusMessage zu replizierende Statusmeldung
     * @return Replikationsnachricht für ein Upsert
     */
    public static StatusReplicationMessage upsert(String sourceNodeId, StatusMessage statusMessage) {
        return new StatusReplicationMessage(
                sourceNodeId,
                StatusEvents.UPSERT,
                statusMessage.getUsername(),
                statusMessage.getStatustext(),
                statusMessage.getUhrzeit(),
                statusMessage.getLatitude(),
                statusMessage.getLongitude()
        );
    }

    /**
     * Erstellt eine DELETE-Replikationsnachricht mit explizitem Löschzeitpunkt.
     *
     * @param sourceNodeId Node-ID des Absenders
     * @param username eindeutiger Benutzername
     * @param deletedAt fachlicher Löschzeitpunkt
     * @return Replikationsnachricht für eine Löschung
     */
    public static StatusReplicationMessage delete(String sourceNodeId, String username, OffsetDateTime deletedAt) {
        return new StatusReplicationMessage(sourceNodeId, StatusEvents.DELETE, username, null, deletedAt, 0, 0);
    }
}
