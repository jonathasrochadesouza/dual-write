package com.dualwrite.lab.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID experimentId,
        UUID orderId,
        String customerId,
        BigDecimal total
) {
}
