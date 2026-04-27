package com.statusserver.status.messaging.replication;

import com.statusserver.status.domain.StatusMessage;
import com.statusserver.status.messaging.StatusChannels;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatusReplicationPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishUpsert(StatusMessage statusMessage) {
        rabbitTemplate.convertAndSend(
                StatusChannels.REPLICATION_EXCHANGE,
                StatusChannels.REPLICATION_ROUTING_KEY,
                StatusReplicationMessage.upsert(statusMessage)
        );
    }

    public void publishDelete(String username) {
        rabbitTemplate.convertAndSend(
                StatusChannels.REPLICATION_EXCHANGE,
                StatusChannels.REPLICATION_ROUTING_KEY,
                StatusReplicationMessage.delete(username)
        );
    }
}
