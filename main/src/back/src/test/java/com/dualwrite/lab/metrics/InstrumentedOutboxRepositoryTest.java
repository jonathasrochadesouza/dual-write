package com.dualwrite.lab.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.dualwrite.lab.outbox.JdbcOutboxRepository;
import com.dualwrite.lab.outbox.OutboxEvent;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class InstrumentedOutboxRepositoryTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final DualWriteMetrics metrics = new DualWriteMetrics(registry);
    private final SavingOutboxRepository delegate = new SavingOutboxRepository();
    private final InstrumentedOutboxRepository repository = new InstrumentedOutboxRepository(delegate, metrics);

    private final OutboxEvent event = OutboxEvent.pending(
            UUID.randomUUID(), UUID.randomUUID(), "OrderCreated", "{}");

    @AfterEach
    void clearScenarioContext() {
        CurrentScenario.clear();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void countsCommittedOutboxRowForCurrentScenario() {
        CurrentScenario.set(ScenarioId.OUTBOX_COMMIT);

        repository.save(event);

        assertEquals(1.0, counterValue());
        assertSame(event, delegate.saved);
    }

    @Test
    void doesNotCountWhenNoScenarioIsActive() {
        repository.save(event);

        assertNull(registry.find("dualwrite.outbox.rows.total").counter());
    }

    @Test
    void defersCountingUntilTransactionCommits() {
        CurrentScenario.set(ScenarioId.OUTBOX_COMMIT);
        TransactionSynchronizationManager.initSynchronization();

        repository.save(event);

        assertEquals(0.0, counterValue());

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        assertEquals(1.0, counterValue());
    }

    @Test
    void discardsCountingWhenTransactionRollsBack() {
        CurrentScenario.set(ScenarioId.OUTBOX_ROLLBACK);
        TransactionSynchronizationManager.initSynchronization();

        repository.save(event);

        assertEquals(0.0, counterValue());
    }

    private double counterValue() {
        var counter = registry.find("dualwrite.outbox.rows.total").counter();
        return counter == null ? 0.0 : counter.count();
    }

    private static class SavingOutboxRepository extends JdbcOutboxRepository {

        private OutboxEvent saved;

        SavingOutboxRepository() {
            super(null);
        }

        @Override
        public OutboxEvent save(OutboxEvent event) {
            this.saved = event;
            return event;
        }
    }
}
