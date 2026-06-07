package com.statusserver.status.sync;

import com.statusserver.status.application.dto.StatusDto;

import java.util.List;

/**
 * Vollständiger Zustand einer Node für den initialen Peer-Sync.
 *
 * @param statuses aktuelle Statusmeldungen
 */
public record StatusSnapshot(
        List<StatusDto> statuses
) {
}
