package com.statusserver.status.application.dto;

import com.statusserver.status.domain.StatusMessage;
import com.statusserver.status.messaging.replication.StatusReplicationMessage;
import java.time.OffsetDateTime;

/**
 * Transportobjekt für Statusmeldungen an REST-, Sync- und Replikationsgrenzen.
 *
 * @param username eindeutiger Benutzername
 * @param statustext textuelle Statusmeldung
 * @param uhrzeit fachlicher Zeitstempel der Änderung
 * @param latitude geographische Breite
 * @param longitude geographische Länge
 */
public record StatusDto(
        String username,
        String statustext,
        OffsetDateTime uhrzeit,
        double latitude,
        double longitude
) {
    /**
     * Wandelt eine persistierte Statusmeldung in ein DTO um.
     *
     * @param message persistierte Statusmeldung
     * @return DTO-Repräsentation
     */
    public static StatusDto from(StatusMessage message) {
        return new StatusDto(
                message.getUsername(),
                message.getStatustext(),
                message.getUhrzeit(),
                message.getLatitude(),
                message.getLongitude()
        );
    }

    /**
     * Wandelt eine Replikationsnachricht in ein Status-DTO um.
     *
     * @param message Replikationsnachricht
     * @return DTO-Repräsentation
     */
    public static StatusDto from(StatusReplicationMessage message) {
        return new StatusDto(
                message.username(),
                message.statustext(),
                message.uhrzeit(),
                message.latitude(),
                message.longitude()
        );
    }

    /**
     * Wandelt dieses DTO in eine JPA-Entität um.
     *
     * @return persistierbare Statusmeldung
     */
    public StatusMessage toEntity() {
        return new StatusMessage(
                username,
                statustext,
                uhrzeit,
                latitude,
                longitude
        );
    }
}
