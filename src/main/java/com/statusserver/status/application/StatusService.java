package com.statusserver.status.application;

import com.statusserver.status.application.dto.StatusDto;
import com.statusserver.status.domain.StatusMessage;
import com.statusserver.status.messaging.StatusChannels;
import com.statusserver.status.messaging.StatusEvents;
import com.statusserver.status.messaging.replication.StatusReplicationMessage;
import com.statusserver.status.messaging.replication.StatusReplicationPublisher;
import com.statusserver.status.persistence.StatusMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StatusService {
    private final StatusMessageRepository repository;
    private final StatusReplicationPublisher replicationPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    public List<StatusDto> findAll() {
        return repository.findAll(Sort.by(
                Sort.Order.desc("uhrzeit"),
                Sort.Order.asc("username")
        )).stream().map(StatusDto::from).toList();
    }

    public Optional<StatusDto> findOne(String username) {
        return repository.findById(username).map(StatusDto::from);
    }

    @Transactional
    public StatusDto upsertFromClient(StatusDto request) {
        StatusMessage saved = saveWithConflictResolution(request);
        replicationPublisher.publishUpsert(saved);
        broadcastChange(saved);
        return StatusDto.from(saved);
    }

    @Transactional
    public void deleteFromClient(String username) {
        if (repository.existsById(username)) {
            repository.deleteById(username);
            replicationPublisher.publishDelete(username);
            broadcastDelete(username);
        }
    }

    @Transactional
    public void applyReplication(StatusReplicationMessage message) {
        if (StatusEvents.DELETE.equalsIgnoreCase(message.eventType())) {
            repository.deleteById(message.username());
            broadcastDelete(message.username());
            return;
        }

        StatusDto request = new StatusDto(
                message.username(),
                message.statustext(),
                message.uhrzeit(),
                message.latitude(),
                message.longitude()
        );

        StatusMessage saved = saveWithConflictResolution(request);
        broadcastChange(saved);
    }

    private StatusMessage saveWithConflictResolution(StatusDto request) {
        Optional<StatusMessage> existing = repository.findById(request.username());
        if (existing.isPresent() && !request.uhrzeit().isAfter(existing.get().getUhrzeit())) {
            return existing.get();
        }

        StatusMessage updated = new StatusMessage(
                request.username(),
                request.statustext(),
                request.uhrzeit(),
                request.latitude(),
                request.longitude()
        );
        return repository.save(updated);
    }

    private void broadcastChange(StatusMessage message) {
        messagingTemplate.convertAndSend(StatusChannels.WS_STATUS_FEED, StatusDto.from(message));
    }

    private void broadcastDelete(String username) {
        messagingTemplate.convertAndSend(StatusChannels.WS_STATUS_EVENTS, StatusEvents.DELETE_PREFIX + username);
    }
}
