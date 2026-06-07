package com.statusserver.status.application;

import com.statusserver.status.application.dto.StatusDto;
import com.statusserver.status.domain.StatusMessage;
import com.statusserver.status.messaging.StatusChannels;
import com.statusserver.status.messaging.StatusEvents;
import com.statusserver.status.messaging.replication.StatusReplicationMessage;
import com.statusserver.status.messaging.replication.StatusReplicationPublisher;
import com.statusserver.status.persistence.StatusMessageRepository;
import com.statusserver.status.sync.StatusSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Fachservice für Statusmeldungen, Konfliktauflösung, Replikation und Snapshot-Sync.
 */
@Service
@RequiredArgsConstructor
public class StatusService {
    private final StatusMessageRepository repository;
    private final StatusReplicationPublisher replicationPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Liefert alle aktuellen Statusmeldungen sortiert nach Aktualität und Benutzername.
     *
     * @return aktuelle Statusmeldungen
     */
    public List<StatusDto> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(StatusMessage::getUhrzeit).reversed()
                        .thenComparing(StatusMessage::getUsername))
                .map(StatusDto::from)
                .toList();
    }

    /**
     * Sucht die aktuelle Statusmeldung eines Benutzers.
     *
     * @param username eindeutiger Benutzername
     * @return optionale Statusmeldung
     */
    public Optional<StatusDto> findOne(String username) {
        return repository.findByUsername(username).map(StatusDto::from);
    }

    /**
     * Speichert eine Client-Änderung, prüft Konflikte und repliziert erfolgreiche Updates.
     *
     * @param request Statusmeldung des Clients
     * @return lokal gültige Statusmeldung nach Konfliktauflösung
     */
    @Transactional
    public StatusDto upsertFromClient(StatusDto request) {
        validateUpsert(request);
        SaveResult result = saveWithConflictResolution(request);
        if (result.changed()) {
            replicationPublisher.publishUpsert(result.message());
            broadcastChange(result.message());
        }
        return StatusDto.from(result.message());
    }

    /**
     * Löscht einen Status aus Client-Sicht und repliziert die Löschung.
     *
     * @param username eindeutiger Benutzername
     */
    @Transactional
    public void deleteFromClient(String username) {
        validateUsername(username);
        OffsetDateTime deletedAt = OffsetDateTime.now();
        DeleteResult result = applyDelete(username, deletedAt);
        if (result.changed()) {
            replicationPublisher.publishDelete(username, deletedAt);
            broadcastDelete(username);
        }
    }

    /**
     * Wendet eine von einer anderen Node empfangene Replikationsnachricht an.
     *
     * @param message empfangene Replikationsnachricht
     */
    @Transactional
    public void applyReplication(StatusReplicationMessage message) {
        if (StatusEvents.DELETE.equalsIgnoreCase(message.eventType())) {
            DeleteResult result = applyDelete(message.username(), message.uhrzeit());
            if (result.changed()) {
                broadcastDelete(message.username());
            }
            return;
        }

        StatusDto request = StatusDto.from(message);
        validateUpsert(request);

        SaveResult result = saveWithConflictResolution(request);
        if (result.changed()) {
            broadcastChange(result.message());
        }
    }

    /**
     * Erstellt einen Snapshot aller Statusmeldungen für bootende Peers.
     *
     * @return vollständiger lokaler Sync-Snapshot
     */
    @Transactional(readOnly = true)
    public StatusSnapshot snapshot() {
        return new StatusSnapshot(findAll());
    }

    /**
     * Importiert einen Peer-Snapshot mit derselben deterministischen Konfliktlogik wie Replikation.
     *
     * @param snapshot Snapshot einer anderen Node
     */
    @Transactional
    public void importSnapshot(StatusSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (snapshot.statuses() != null) {
            snapshot.statuses().forEach(status -> {
                validateUpsert(status);
                saveWithConflictResolution(status);
            });
        }

    }

    /**
     * Speichert ein Update nur, wenn es neuer als der vorhandene Status ist.
     *
     * @param request eingehendes Status-Update
     * @return Ergebnis mit gespeicherter Meldung und Änderungsflag
     */
    private SaveResult saveWithConflictResolution(StatusDto request) {
        Optional<StatusMessage> existing = repository.findById(request.username());
        if (existing.isPresent() && !request.uhrzeit().isAfter(existing.get().getUhrzeit())) {
            return new SaveResult(existing.get(), false);
        }

        StatusMessage updated = request.toEntity();
        return new SaveResult(repository.save(updated), true);
    }

    /**
     * Wendet eine Löschung mit Zeitstempel an.
     *
     * @param username eindeutiger Benutzername
     * @param deletedAt fachlicher Löschzeitpunkt
     * @return Ergebnis mit Änderungsflag
     */
    private DeleteResult applyDelete(String username, OffsetDateTime deletedAt) {
        validateUsername(username);
        if (deletedAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "uhrzeit is required for delete replication");
        }

        boolean exists = repository.existsById(username);
        if (!exists) {
            return new DeleteResult(false);
        }

        repository.deleteById(username);
        return new DeleteResult(true);
    }

    /**
     * Validiert die Pflichtfelder und Koordinaten eines Status-Updates.
     *
     * @param request zu prüfende Statusmeldung
     */
    private void validateUpsert(StatusDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status body is required");
        }

        validateUsername(request.username());

        if (request.statustext() == null || request.statustext().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "statustext is required");
        }

        if (request.uhrzeit() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "uhrzeit is required");
        }

        if (request.latitude() < -90 || request.latitude() > 90) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude must be between -90 and 90");
        }

        if (request.longitude() < -180 || request.longitude() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "longitude must be between -180 and 180");
        }
    }

    /**
     * Validiert, dass ein Benutzername gesetzt ist.
     *
     * @param username zu prüfender Benutzername
     */
    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
    }

    /**
     * Ergebnis eines speichernden Upserts nach Konfliktauflösung.
     *
     * @param message gültige Statusmeldung
     * @param changed true, wenn der lokale Zustand geändert wurde
     */
    private record SaveResult(StatusMessage message, boolean changed) {
    }

    /**
     * Ergebnis einer angewendeten Löschung.
     *
     * @param changed true, wenn ein Status gelöscht wurde
     */
    private record DeleteResult(boolean changed) {
    }

    /**
     * Sendet eine geänderte Statusmeldung an WebSocket-Clients.
     *
     * @param message geänderte Statusmeldung
     */
    private void broadcastChange(StatusMessage message) {
        messagingTemplate.convertAndSend(StatusChannels.WS_STATUS_FEED, StatusDto.from(message));
    }

    /**
     * Sendet ein Löschereignis an WebSocket-Clients.
     *
     * @param username Benutzername des gelöschten Status
     */
    private void broadcastDelete(String username) {
        messagingTemplate.convertAndSend(StatusChannels.WS_STATUS_EVENTS, StatusEvents.DELETE_PREFIX + username);
    }
}
