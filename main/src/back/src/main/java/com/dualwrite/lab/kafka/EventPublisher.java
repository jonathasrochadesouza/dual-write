package com.dualwrite.lab.kafka;

public interface EventPublisher {
    void publish(OrderCreatedEvent event);
}
