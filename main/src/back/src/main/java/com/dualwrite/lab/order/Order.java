package com.dualwrite.lab.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Order(
        UUID id,
        UUID experimentId,
        String customerId,
        BigDecimal total,
        String status,
        Instant createdAt
) {
    public static Order create(UUID experimentId, String customerId, BigDecimal total) {
        return new Order(
                UUID.randomUUID(),
                experimentId,
                customerId,
                total,
                "CREATED",
                Instant.now()
        );
    }
}
