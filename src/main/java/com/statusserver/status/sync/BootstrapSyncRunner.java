package com.statusserver.status.sync;

import com.statusserver.config.AppProperties;
import com.statusserver.status.application.StatusService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Führt beim Start einer Node den initialen Snapshot-Sync gegen konfigurierte Peers aus.
 */
@Component
@RequiredArgsConstructor
public class BootstrapSyncRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapSyncRunner.class);

    private final AppProperties properties;
    private final StatusService statusService;
    private final BootstrapState bootstrapState;
    private final RestTemplateBuilder restTemplateBuilder;

    /**
     * Startet die Grace-Period, synchronisiert von erreichbaren Peers und markiert die Node danach als bereit.
     * Wenn ein Peer erforderlich ist, wird bis zu einem erfolgreichen Sync weiter versucht.
     *
     * @param args Startargumente der Anwendung
     * @throws Exception falls der Thread-Sleep während der Retry-Schleife unterbrochen wird
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> peerBaseUrls = properties.getPeerBaseUrls().stream()
                .filter(peerBaseUrl -> peerBaseUrl != null && !peerBaseUrl.isBlank())
                .toList();

        if (peerBaseUrls.isEmpty()) {
            bootstrapState.markReady();
            return;
        }

        Instant deadline = Instant.now().plus(properties.getBootstrapTimeout());
        boolean synced = false;

        while (!synced) {
            for (String peerBaseUrl : peerBaseUrls) {
                synced = syncFromPeer(peerBaseUrl);
                if (synced) {
                    break;
                }
            }

            if (!synced && !properties.isBootstrapRequirePeer() && !Instant.now().isBefore(deadline)) {
                log.warn("Bootstrap sync finished without reachable peers after {}", properties.getBootstrapTimeout());
                break;
            }

            if (!synced && properties.isBootstrapRequirePeer() && !Instant.now().isBefore(deadline)) {
                log.warn("Bootstrap sync still waiting for a reachable peer after {}", properties.getBootstrapTimeout());
                deadline = Instant.now().plus(properties.getBootstrapTimeout());
            }

            if (!synced) {
                Thread.sleep(properties.getBootstrapRetryDelay().toMillis());
            }
        }

        bootstrapState.markReady();
    }

    /**
     * Versucht, einen Snapshot von einem einzelnen Peer zu laden und lokal zu importieren.
     *
     * @param peerBaseUrl Basis-URL der Peer-Node
     * @return {@code true}, wenn der Sync erfolgreich war
     */
    private boolean syncFromPeer(String peerBaseUrl) {
        try {
            StatusSnapshot snapshot = restTemplateBuilder
                    .setConnectTimeout(Duration.ofSeconds(2))
                    .setReadTimeout(Duration.ofSeconds(5))
                    .build()
                    .getForObject(peerBaseUrl + "/internal/status-sync/snapshot", StatusSnapshot.class);
            statusService.importSnapshot(snapshot);
            log.info("Bootstrap sync imported snapshot from {}", peerBaseUrl);
            return true;
        } catch (RuntimeException ex) {
            log.info("Bootstrap sync could not reach {}: {}", peerBaseUrl, ex.getMessage());
            return false;
        }
    }
}
