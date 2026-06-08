package com.statusserver.status.sync;

import com.statusserver.status.domain.StatusTombstone;

import java.time.OffsetDateTime;

/**
 * Tombstone-Repräsentation für Snapshot-Sync und Reconciliation.
 *
 * @param username eindeutiger Benutzername
 * @param deletedAt fachlicher Löschzeitpunkt
 */
public record StatusTombstoneDto(
        String username,
        OffsetDateTime deletedAt
) {
    /**
     * Wandelt einen persistierten Tombstone in ein DTO.
     *
     * @param tombstone persistierter Tombstone
     * @return DTO-Repräsentation
     */
    public static StatusTombstoneDto from(StatusTombstone tombstone) {
        return new StatusTombstoneDto(tombstone.getUsername(), tombstone.getDeletedAt());
    }
}
