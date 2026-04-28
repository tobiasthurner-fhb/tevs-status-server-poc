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
        SaveResult result = saveWithConflictResolution(request);
        if (result.changed()) {
            replicationPublisher.publishUpsert(result.message());
            broadcastChange(result.message());
        }
        return StatusDto.from(result.message());
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
            if (repository.existsById(message.username())) {
                repository.deleteById(message.username());
                broadcastDelete(message.username());
            }
            return;
        }

        StatusDto request = StatusDto.from(message);

        SaveResult result = saveWithConflictResolution(request);
        if (result.changed()) {
            broadcastChange(result.message());
        }
    }

    private SaveResult saveWithConflictResolution(StatusDto request) {
        Optional<StatusMessage> existing = repository.findById(request.username());
        if (existing.isPresent() && !request.uhrzeit().isAfter(existing.get().getUhrzeit())) {
            return new SaveResult(existing.get(), false);
        }

        StatusMessage updated = request.toEntity();
        return new SaveResult(repository.save(updated), true);
    }

    private record SaveResult(StatusMessage message, boolean changed) {
    }

    private void broadcastChange(StatusMessage message) {
        messagingTemplate.convertAndSend(StatusChannels.WS_STATUS_FEED, StatusDto.from(message));
    }

    private void broadcastDelete(String username) {
        messagingTemplate.convertAndSend(StatusChannels.WS_STATUS_EVENTS, StatusEvents.DELETE_PREFIX + username);
    }
}
