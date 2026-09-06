package com.dualwrite.lab.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.dualwrite.lab.order.JdbcOrderRepository;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class InstrumentedOrderRepositoryTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final DualWriteMetrics metrics = new DualWriteMetrics(registry);
    private final SavingOrderRepository delegate = new SavingOrderRepository();
    private final InstrumentedOrderRepository repository = new InstrumentedOrderRepository(delegate, metrics);

    private final Order order = Order.create(UUID.randomUUID(), "customer-42", new BigDecimal("199.90"));

    @AfterEach
    void clearScenarioContext() {
        CurrentScenario.clear();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void countsCommittedDatabaseWriteForCurrentScenario() {
        CurrentScenario.set(ScenarioId.DUAL_WRITE_BOTH_OK);

        repository.save(order);

        assertEquals(1.0, counterValue());
        assertSame(order, delegate.saved);
    }

    @Test
    void doesNotCountWhenNoScenarioIsActive() {
        repository.save(order);

        assertNull(registry.find("dualwrite.orders.total").counter());
        assertSame(order, delegate.saved);
    }

    @Test
    void defersCountingUntilTransactionCommits() {
        CurrentScenario.set(ScenarioId.DUAL_WRITE_NONE);
        TransactionSynchronizationManager.initSynchronization();

        repository.save(order);

        assertEquals(0.0, counterValue());

        commitActiveSynchronizations();

        assertEquals(1.0, counterValue());
    }

    @Test
    void discardsCountingWhenTransactionRollsBack() {
        CurrentScenario.set(ScenarioId.OUTBOX_ROLLBACK);
        TransactionSynchronizationManager.initSynchronization();

        repository.save(order);

        assertEquals(0.0, counterValue());
    }

    @Test
    void delegatesReadOperationsWithoutCounting() {
        UUID experimentId = UUID.randomUUID();

        repository.countByExperimentId(experimentId);

        assertEquals(1L, delegate.countByExperimentIdCalls);
        assertNull(registry.find("dualwrite.orders.total").counter());
    }

    private double counterValue() {
        var counter = registry.find("dualwrite.orders.total").counter();
        return counter == null ? 0.0 : counter.count();
    }

    private static void commitActiveSynchronizations() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
    }

    private static class SavingOrderRepository extends JdbcOrderRepository {

        private Order saved;
        private long countByExperimentIdCalls;

        SavingOrderRepository() {
            super(null);
        }

        @Override
        public Order save(Order order) {
            this.saved = order;
            return order;
        }

        @Override
        public long countByExperimentId(UUID experimentId) {
            countByExperimentIdCalls++;
            return 0L;
        }
    }
}
