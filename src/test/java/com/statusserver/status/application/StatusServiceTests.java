package com.statusserver.status.application;

import com.statusserver.status.application.dto.StatusDto;
import com.statusserver.status.messaging.replication.StatusReplicationMessage;
import com.statusserver.status.messaging.replication.StatusReplicationPublisher;
import com.statusserver.status.persistence.StatusMessageRepository;
import com.statusserver.status.persistence.StatusTombstoneRepository;
import com.statusserver.status.sync.StatusSnapshot;
import com.statusserver.status.sync.StatusTombstoneDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fachtests für Konfliktauflösung, Snapshot-Import und Validierung.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:service-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.main.allow-bean-definition-overriding=true",
        "app.peer-base-urls="
})
class StatusServiceTests {
    @Autowired
    private StatusService statusService;

    @Autowired
    private StatusMessageRepository statusRepository;

    @Autowired
    private StatusTombstoneRepository tombstoneRepository;

    /**
     * Bereinigt Status-Tabellen vor jedem Testfall.
     */
    @BeforeEach
    void setUp() {
        statusRepository.deleteAll();
        tombstoneRepository.deleteAll();
    }

    /**
     * Prüft, dass eine ältere replizierte Meldung keinen neueren lokalen Status überschreibt.
     */
    @Test
    void rejectsOlderReplicatedUpdate() {
        OffsetDateTime newer = OffsetDateTime.parse("2026-03-03T13:30:00+01:00");
        OffsetDateTime older = newer.minusMinutes(1);

        statusService.upsertFromClient(status("RECON-01", "new", newer));
        statusService.applyReplication(StatusReplicationMessage.upsert("node-2", status("RECON-01", "old", older).toEntity()));

        assertThat(statusService.findOne("RECON-01")).get().extracting(StatusDto::statustext).isEqualTo("new");
    }

    /**
     * Prüft, dass eine Löschung eine vorhandene Statusmeldung entfernt.
     */
    @Test
    void replicatedDeleteRemovesExistingStatus() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-03T13:30:00+01:00");
        OffsetDateTime deletedAt = createdAt.plusMinutes(1);

        statusService.upsertFromClient(status("RECON-01", "active", createdAt));
        statusService.applyReplication(StatusReplicationMessage.delete("node-2", "RECON-01", deletedAt));

        assertThat(statusService.findOne("RECON-01")).isEmpty();
    }

    /**
     * Prüft, dass Snapshot-Import Statusmeldungen übernimmt.
     */
    @Test
    void importsSnapshotWithStatuses() {
        OffsetDateTime statusTime = OffsetDateTime.parse("2026-03-03T13:30:00+01:00");
        StatusSnapshot snapshot = new StatusSnapshot(List.of(status("RECON-01", "synced", statusTime)), List.of());

        statusService.importSnapshot(snapshot);

        assertThat(statusService.findOne("RECON-01")).isPresent();
    }

    /**
     * Prüft, dass ein partieller Peer-Snapshot lokale Statusmeldungen nicht blind entfernt.
     */
    @Test
    void importSnapshotDoesNotRemoveStatusesMissingFromPeerSnapshot() {
        OffsetDateTime statusTime = OffsetDateTime.parse("2026-03-03T13:30:00+01:00");

        statusService.upsertFromClient(status("RECON-01", "local", statusTime));
        statusService.importSnapshot(new StatusSnapshot(List.of(), List.of()));

        assertThat(statusService.findOne("RECON-01")).isPresent();
    }

    /**
     * Prüft, dass ein Delete-Tombstone ältere Status aus Snapshots blockiert.
     */
    @Test
    void tombstonePreventsOlderSnapshotStatusFromReappearing() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-03T13:30:00+01:00");
        OffsetDateTime deletedAt = createdAt.plusMinutes(1);

        statusService.applyReplication(StatusReplicationMessage.delete("node-2", "RECON-01", deletedAt));
        statusService.importSnapshot(new StatusSnapshot(List.of(status("RECON-01", "old", createdAt)), List.of()));

        assertThat(statusService.findOne("RECON-01")).isEmpty();
        assertThat(tombstoneRepository.findById("RECON-01")).isPresent();
    }

    /**
     * Prüft, dass Tombstones über Snapshots auf andere Nodes übertragen werden.
     */
    @Test
    void snapshotTombstoneDeletesOlderLocalStatus() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-03T13:30:00+01:00");
        OffsetDateTime deletedAt = createdAt.plusMinutes(1);

        statusService.upsertFromClient(status("RECON-01", "local", createdAt));
        statusService.importSnapshot(new StatusSnapshot(List.of(), List.of(new StatusTombstoneDto("RECON-01", deletedAt))));

        assertThat(statusService.findOne("RECON-01")).isEmpty();
    }

    /**
     * Prüft, dass ein bewusst neuerer Status nach einer Löschung wieder gültig gesetzt werden kann.
     */
    @Test
    void newerStatusCanReplaceTombstone() {
        OffsetDateTime deletedAt = OffsetDateTime.parse("2026-03-03T13:30:00+01:00");
        OffsetDateTime recreatedAt = deletedAt.plusMinutes(1);

        statusService.applyReplication(StatusReplicationMessage.delete("node-2", "RECON-01", deletedAt));
        statusService.upsertFromClient(status("RECON-01", "recreated", recreatedAt));

        assertThat(statusService.findOne("RECON-01")).get().extracting(StatusDto::statustext).isEqualTo("recreated");
        assertThat(tombstoneRepository.findById("RECON-01")).isEmpty();
    }

    /**
     * Prüft die Mindestanforderung von 10 simultanen Clients mit parallelen Writes.
     */
    @Test
    void supportsAtLeastTenSimultaneousClients() throws Exception {
        OffsetDateTime baseTime = OffsetDateTime.parse("2026-03-03T13:30:00+01:00");
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            List<Callable<Void>> clients = java.util.stream.IntStream.rangeClosed(1, 10)
                    .mapToObj(clientId -> (Callable<Void>) () -> {
                        statusService.upsertFromClient(status("RECON-%02d".formatted(clientId), "client-" + clientId,
                                baseTime.plusSeconds(clientId)));
                        return null;
                    })
                    .toList();

            for (Future<Void> result : executor.invokeAll(clients)) {
                result.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(statusService.findAll()).hasSize(10);
    }

    /**
     * Prüft die fachliche Validierung ungültiger Koordinaten.
     */
    @Test
    void validatesCoordinates() {
        StatusDto invalid = status("RECON-01", "invalid", OffsetDateTime.now(), 91, 16);

        assertThatThrownBy(() -> statusService.upsertFromClient(invalid))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("latitude");
    }

    /**
     * Erstellt ein Standard-Test-DTO mit Wiener Beispielkoordinaten.
     *
     * @param username eindeutiger Benutzername
     * @param text Statusmeldung
     * @param timestamp fachlicher Zeitstempel
     * @return Test-Statusmeldung
     */
    private StatusDto status(String username, String text, OffsetDateTime timestamp) {
        return status(username, text, timestamp, 48.215, 16.385);
    }

    /**
     * Erstellt ein Test-DTO mit expliziten Koordinaten.
     *
     * @param username eindeutiger Benutzername
     * @param text Statusmeldung
     * @param timestamp fachlicher Zeitstempel
     * @param latitude geographische Breite
     * @param longitude geographische Länge
     * @return Test-Statusmeldung
     */
    private StatusDto status(String username, String text, OffsetDateTime timestamp, double latitude, double longitude) {
        return new StatusDto(username, text, timestamp, latitude, longitude);
    }

    /**
     * Testkonfiguration ohne echte RabbitMQ- und WebSocket-Nebenwirkungen.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {
        /**
         * Ersetzt den Replikations-Publisher durch eine No-Op-Implementierung.
         *
         * @return No-Op-Publisher
         */
        @Bean
        @Primary
        StatusReplicationPublisher noOpStatusReplicationPublisher() {
            return new NoOpStatusReplicationPublisher();
        }

        /**
         * Ersetzt das WebSocket-Template durch ein Template mit erfolgreichem Dummy-Channel.
         *
         * @return testbares SimpMessagingTemplate
         */
        @Bean
        @Primary
        SimpMessagingTemplate noOpSimpMessagingTemplate() {
            MessageChannel channel = (message, timeout) -> true;
            return new SimpMessagingTemplate(channel);
        }
    }

    /**
     * No-Op-Publisher für Servicetests ohne RabbitMQ-Zugriff.
     */
    private static class NoOpStatusReplicationPublisher extends StatusReplicationPublisher {
        /**
         * Erstellt den Publisher ohne RabbitTemplate, da alle Publish-Methoden überschrieben sind.
         */
        NoOpStatusReplicationPublisher() {
            super(null);
        }

        /**
         * Ignoriert Upsert-Replikation im Test.
         *
         * @param statusMessage gespeicherte Statusmeldung
         */
        @Override
        public void publishUpsert(com.statusserver.status.domain.StatusMessage statusMessage) {
        }

        /**
         * Ignoriert Delete-Replikation im Test.
         *
         * @param username Benutzername des gelöschten Status
         * @param deletedAt fachlicher Löschzeitpunkt
         */
        @Override
        public void publishDelete(String username, OffsetDateTime deletedAt) {
        }
    }
}
