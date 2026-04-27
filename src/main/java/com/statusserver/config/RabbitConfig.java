package com.statusserver.config;

import com.statusserver.status.messaging.StatusChannels;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Value("${app.node-id:default-node}")
    private String nodeId;

    @Bean
    public TopicExchange replicationExchange() {
        return new TopicExchange(StatusChannels.REPLICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue replicationQueue() {
        return new Queue(StatusChannels.REPLICATION_QUEUE_PREFIX + nodeId, true);
    }

    @Bean
    public Binding replicationBinding(Queue replicationQueue, TopicExchange replicationExchange) {
        return BindingBuilder.bind(replicationQueue).to(replicationExchange).with(StatusChannels.REPLICATION_ROUTING_KEY);
    }
}
