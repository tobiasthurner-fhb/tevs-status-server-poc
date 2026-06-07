package com.statusserver.status.sync;

import com.statusserver.status.application.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Interner Controller für Snapshot-Anfragen anderer Nodes.
 */
@RestController
@RequestMapping("/internal/status-sync")
@RequiredArgsConstructor
public class StatusSyncController {
    private final StatusService statusService;

    /**
     * Liefert den lokalen Status-Snapshot für bootende Peers.
     *
     * @return vollständiger lokaler Snapshot
     */
    @GetMapping("/snapshot")
    public StatusSnapshot snapshot() {
        return statusService.snapshot();
    }
}
