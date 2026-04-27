package com.statusserver.status.application.dto;

import com.statusserver.status.domain.StatusMessage;
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
}
