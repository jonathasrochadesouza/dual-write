package com.dualwrite.lab.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.dualwrite.lab.kafka.KafkaEventPublisher;
import com.dualwrite.lab.kafka.OrderCreatedEvent;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InstrumentedEventPublisherTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final DualWriteMetrics metrics = new DualWriteMetrics(registry);
    private final PublishingEventPublisher delegate = new PublishingEventPublisher();
    private final InstrumentedEventPublisher publisher = new InstrumentedEventPublisher(delegate, metrics);

    private final OrderCreatedEvent event = new OrderCreatedEvent(
            UUID.randomUUID(), UUID.randomUUID(), "customer-42", new BigDecimal("199.90"));

    @AfterEach
    void clearScenarioContext() {
        CurrentScenario.clear();
    }

    @Test
    void countsPublicationAndMeasuresLatencyForCurrentScenario() {
        CurrentScenario.set(ScenarioId.DUAL_WRITE_BOTH_OK);

        publisher.publish(event);

        assertSame(event, delegate.published);
        assertEquals(1.0, registry.get("dualwrite.events.published.total")
                .tag("scenario", ScenarioId.DUAL_WRITE_BOTH_OK.name())
                .counter()
                .count());
        assertEquals(1L, registry.get("dualwrite.kafka.publish.latency")
                .tag("scenario", ScenarioId.DUAL_WRITE_BOTH_OK.name())
                .timer()
                .count());
    }

    @Test
    void doesNotMeasureWhenNoScenarioIsActive() {
        publisher.publish(event);

        assertSame(event, delegate.published);
        assertNull(registry.find("dualwrite.events.published.total").counter());
        assertNull(registry.find("dualwrite.kafka.publish.latency").timer());
    }

    private static class PublishingEventPublisher extends KafkaEventPublisher {

        private OrderCreatedEvent published;

        PublishingEventPublisher() {
            super(null, null, "dual-write-lab");
        }

        @Override
        public void publish(OrderCreatedEvent event) {
            this.published = event;
        }
    }
}
