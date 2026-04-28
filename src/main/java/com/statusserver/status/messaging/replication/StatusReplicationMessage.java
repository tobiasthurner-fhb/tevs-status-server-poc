package com.statusserver.status.messaging.replication;

import com.statusserver.status.domain.StatusMessage;
import com.statusserver.status.messaging.StatusEvents;
import java.time.OffsetDateTime;

public record StatusReplicationMessage(
        String sourceNodeId,
        String eventType,
        String username,
        String statustext,
        OffsetDateTime uhrzeit,
        double latitude,
        double longitude
) {
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

    public static StatusReplicationMessage delete(String sourceNodeId, String username) {
        return new StatusReplicationMessage(sourceNodeId, StatusEvents.DELETE, username, null, null, 0, 0);
    }
}
