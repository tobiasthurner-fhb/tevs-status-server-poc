package com.statusserver.status.messaging.replication;

import com.statusserver.status.domain.StatusMessage;
import com.statusserver.status.messaging.StatusChannels;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Veröffentlicht lokale Statusänderungen als Replikationsnachrichten.
 */
@Component
@RequiredArgsConstructor
public class StatusReplicationPublisher {
    private static final Logger log = LoggerFactory.getLogger(StatusReplicationPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.node-id:default-node}")
    private String nodeId;

    /**
     * Veröffentlicht ein Upsert-Ereignis für andere Nodes.
     *
     * @param statusMessage gespeicherte Statusmeldung
     */
    public void publishUpsert(StatusMessage statusMessage) {
        try {
            rabbitTemplate.convertAndSend(
                    StatusChannels.REPLICATION_EXCHANGE,
                    StatusChannels.REPLICATION_ROUTING_KEY,
                    StatusReplicationMessage.upsert(nodeId, statusMessage)
            );
        } catch (AmqpException ex) {
            log.warn("Could not publish upsert for {}. Other nodes will not receive this event via RabbitMQ: {}",
                    statusMessage.getUsername(), ex.getMessage());
        }
    }

    /**
     * Veröffentlicht ein Delete-Ereignis mit Löschzeitstempel.
     *
     * @param username Benutzername des gelöschten Status
     * @param deletedAt fachlicher Löschzeitpunkt
     */
    public void publishDelete(String username, OffsetDateTime deletedAt) {
        try {
            rabbitTemplate.convertAndSend(
                    StatusChannels.REPLICATION_EXCHANGE,
                    StatusChannels.REPLICATION_ROUTING_KEY,
                    StatusReplicationMessage.delete(nodeId, username, deletedAt)
            );
        } catch (AmqpException ex) {
            log.warn("Could not publish delete for {}. Other nodes will not receive this event via RabbitMQ: {}",
                    username, ex.getMessage());
        }
    }
}
