package com.statusserver.config;

import com.statusserver.status.messaging.StatusChannels;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Erstellt die RabbitMQ-Infrastruktur für die Node-zu-Node-Replikation.
 */
@Configuration
public class RabbitConfig {
    @Value("${app.node-id:default-node}")
    private String nodeId;

    /**
     * Deklariert den persistenten Exchange für Status-Replikationsereignisse.
     *
     * @return Topic-Exchange für Replikationsnachrichten
     */
    @Bean
    public TopicExchange replicationExchange() {
        return new TopicExchange(StatusChannels.REPLICATION_EXCHANGE, true, false);
    }

    /**
     * Deklariert eine transiente Queue pro Node (autoDelete=true, durable=false),
     * sodass RabbitMQ nur als Transportmedium dient und keine Nachrichten puffert,
     * wenn die Node offline ist. Fehlende Updates werden beim Neustart per
     * Bootstrap-Sync von den Peers nachgeladen.
     *
     * @return node-spezifische Replikations-Queue
     */
    @Bean
    public Queue replicationQueue() {
        return new Queue(StatusChannels.REPLICATION_QUEUE_PREFIX + nodeId, false, false, true);
    }

    /**
     * Bindet die node-spezifische Queue an den Replikations-Exchange.
     *
     * @param replicationQueue node-spezifische Queue
     * @param replicationExchange gemeinsamer Replikations-Exchange
     * @return Binding für Replikationsnachrichten
     */
    @Bean
    public Binding replicationBinding(Queue replicationQueue, TopicExchange replicationExchange) {
        return BindingBuilder.bind(replicationQueue).to(replicationExchange).with(StatusChannels.REPLICATION_ROUTING_KEY);
    }
}
