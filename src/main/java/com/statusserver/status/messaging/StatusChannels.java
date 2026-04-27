package com.statusserver.status.messaging;

public final class StatusChannels {
    public static final String REPLICATION_EXCHANGE = "status.replication.exchange";
    public static final String REPLICATION_ROUTING_KEY = "status.changed";
    public static final String REPLICATION_QUEUE_PREFIX = "status.replication.queue.";

    public static final String WS_TOPIC_PREFIX = "/topic";
    public static final String WS_STATUS_FEED = "/topic/status-feed";
    public static final String WS_STATUS_EVENTS = "/topic/status-events";
    public static final String WS_APP_PREFIX = "/app";
    public static final String WS_ENDPOINT = "/ws";

    private StatusChannels() {
    }
}
