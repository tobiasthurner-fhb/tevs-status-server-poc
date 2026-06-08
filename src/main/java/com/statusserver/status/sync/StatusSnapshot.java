package com.statusserver.status.sync;

import com.statusserver.status.application.dto.StatusDto;

import java.util.List;

/**
 * Vollständiger Zustand einer Node für initialen Sync und laufende Reconciliation.
 *
 * @param statuses aktuelle Statusmeldungen
 * @param tombstones bekannte Löschmarken
 */
public record StatusSnapshot(
        List<StatusDto> statuses,
        List<StatusTombstoneDto> tombstones
) {
}
