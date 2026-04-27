package com.statusserver.status.messaging.replication;

import com.statusserver.status.application.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatusReplicationListener {
    private final StatusService statusService;

    @RabbitListener(queues = "#{@replicationQueue.name}")
    public void onReplicationMessage(StatusReplicationMessage message) {
        statusService.applyReplication(message);
    }
}
