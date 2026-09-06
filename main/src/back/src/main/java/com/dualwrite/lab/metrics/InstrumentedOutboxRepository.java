package com.dualwrite.lab.metrics;

import com.dualwrite.lab.outbox.JdbcOutboxRepository;
import com.dualwrite.lab.outbox.OutboxEvent;
import com.dualwrite.lab.outbox.OutboxRepository;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Counts outbox rows committed through {@link OutboxRepository}, tagged with
 * the scenario from {@link CurrentScenario}. Rolled-back outbox rows are
 * never counted; saves that happen outside a scenario execution are
 * delegated without being counted.
 */
@Component
@Primary
public class InstrumentedOutboxRepository implements OutboxRepository {

    private final JdbcOutboxRepository delegate;
    private final DualWriteMetrics metrics;

    public InstrumentedOutboxRepository(JdbcOutboxRepository delegate, DualWriteMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEvent saved = delegate.save(event);
        ScenarioId scenario = CurrentScenario.current();
        if (scenario != null) {
            CommittedWrites.record(() -> metrics.recordOutboxCommitted(scenario));
        }
        return saved;
    }

    @Override
    public List<OutboxEvent> findByExperimentId(UUID experimentId) {
        return delegate.findByExperimentId(experimentId);
    }

    @Override
    public long countByExperimentId(UUID experimentId) {
        return delegate.countByExperimentId(experimentId);
    }

    @Override
    public void deleteAll() {
        delegate.deleteAll();
    }
}
