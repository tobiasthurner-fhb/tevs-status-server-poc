package com.statusserver.status.messaging.replication;

import com.statusserver.status.application.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Konsumiert Replikationsnachrichten anderer Nodes aus RabbitMQ.
 */
@Component
@RequiredArgsConstructor
public class StatusReplicationListener {
    private final StatusService statusService;

    @Value("${app.node-id:default-node}")
    private String nodeId;

    /**
     * Wendet fremde Replikationsnachrichten an und ignoriert eigene Echo-Nachrichten.
     *
     * @param message empfangene Replikationsnachricht
     */
    @RabbitListener(queues = "#{@replicationQueue.name}")
    public void onReplicationMessage(StatusReplicationMessage message) {
        if (nodeId.equals(message.sourceNodeId())) {
            return;
        }

        statusService.applyReplication(message);
    }
}
