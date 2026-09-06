package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;

import com.dualwrite.lab.kafka.OrderCreatedEvent;
import com.dualwrite.lab.order.Order;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ScenarioPayloadFactory {

    private final ObjectMapper objectMapper;

    public ScenarioPayloadFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(OrderCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize event payload", ex);
        }
    }

    public OrderCreatedEvent toEvent(Order order) {
        return new OrderCreatedEvent(
                order.experimentId(),
                order.id(),
                order.customerId(),
                order.total()
        );
    }
}
