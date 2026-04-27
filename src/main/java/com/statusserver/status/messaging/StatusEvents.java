package com.statusserver.status.messaging;

public final class StatusEvents {
    public static final String UPSERT = "UPSERT";
    public static final String DELETE = "DELETE";
    public static final String DELETE_PREFIX = "DELETED:";

    private StatusEvents() {
    }
}
