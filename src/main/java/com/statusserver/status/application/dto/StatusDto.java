package com.statusserver.status.application.dto;

import com.statusserver.status.domain.StatusMessage;
import com.statusserver.status.messaging.replication.StatusReplicationMessage;
import java.time.OffsetDateTime;

public record StatusDto(
        String username,
        String statustext,
        OffsetDateTime uhrzeit,
        double latitude,
        double longitude
) {
    public static StatusDto from(StatusMessage message) {
        return new StatusDto(
                message.getUsername(),
                message.getStatustext(),
                message.getUhrzeit(),
                message.getLatitude(),
                message.getLongitude()
        );
    }

    public static StatusDto from(StatusReplicationMessage message) {
        return new StatusDto(
                message.username(),
                message.statustext(),
                message.uhrzeit(),
                message.latitude(),
                message.longitude()
        );
    }

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
