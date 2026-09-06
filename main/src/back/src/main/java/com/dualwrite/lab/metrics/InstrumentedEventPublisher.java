package com.dualwrite.lab.metrics;

import com.dualwrite.lab.kafka.EventPublisher;
import com.dualwrite.lab.kafka.KafkaEventPublisher;
import com.dualwrite.lab.kafka.OrderCreatedEvent;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Counts Kafka publications and measures their latency for the scenario from
 * {@link CurrentScenario}. Publications that happen outside a scenario
 * execution are delegated without being measured.
 */
@Component
@Primary
public class InstrumentedEventPublisher implements EventPublisher {

    private final KafkaEventPublisher delegate;
    private final DualWriteMetrics metrics;

    public InstrumentedEventPublisher(KafkaEventPublisher delegate, DualWriteMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        ScenarioId scenario = CurrentScenario.current();
        if (scenario == null) {
            delegate.publish(event);
            return;
        }
        var sample = metrics.startKafkaTimer();
        delegate.publish(event);
        metrics.stopKafkaTimer(sample, scenario);
        metrics.recordKafkaPublish(scenario);
    }
}
