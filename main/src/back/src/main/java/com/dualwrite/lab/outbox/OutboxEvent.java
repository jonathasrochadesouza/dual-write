package com.dualwrite.lab.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        UUID experimentId,
        UUID aggregateId,
        String eventType,
        String payload,
        String status,
        Instant createdAt
) {
    public static OutboxEvent pending(UUID experimentId, UUID aggregateId, String eventType, String payload) {
        return new OutboxEvent(
                UUID.randomUUID(),
                experimentId,
                aggregateId,
                eventType,
                payload,
                "PENDING",
                Instant.now()
        );
    }
}
