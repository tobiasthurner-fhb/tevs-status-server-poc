package com.statusserver.status.messaging.replication;

import com.statusserver.status.domain.StatusMessage;
import com.statusserver.status.messaging.StatusEvents;
import java.time.OffsetDateTime;

public record StatusReplicationMessage(
        String eventType,
        String username,
        String statustext,
        OffsetDateTime uhrzeit,
        double latitude,
        double longitude
) {
    public static StatusReplicationMessage upsert(StatusMessage statusMessage) {
        return new StatusReplicationMessage(
                StatusEvents.UPSERT,
                statusMessage.getUsername(),
                statusMessage.getStatustext(),
                statusMessage.getUhrzeit(),
                statusMessage.getLatitude(),
                statusMessage.getLongitude()
        );
    }

    public static StatusReplicationMessage delete(String username) {
        return new StatusReplicationMessage(StatusEvents.DELETE, username, null, null, 0, 0);
    }
}
