package com.dualwrite.lab.report;

import java.util.List;

import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.outbox.OutboxEvent;

public record ObservedState(
        List<Order> orders,
        List<OutboxEvent> outboxEvents,
        List<String> kafkaPayloads
) {
    public long orderCount() {
        return orders.size();
    }

    public long outboxCount() {
        return outboxEvents.size();
    }

    public long kafkaCount() {
        return kafkaPayloads.size();
    }

    public boolean orderPresent() {
        return !orders.isEmpty();
    }

    public boolean outboxPresent() {
        return !outboxEvents.isEmpty();
    }

    public boolean kafkaPresent() {
        return !kafkaPayloads.isEmpty();
    }
}
