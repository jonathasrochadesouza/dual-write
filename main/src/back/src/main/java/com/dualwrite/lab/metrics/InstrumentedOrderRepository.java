package com.dualwrite.lab.metrics;

import com.dualwrite.lab.order.JdbcOrderRepository;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Counts committed database writes performed through {@link OrderRepository},
 * tagged with the scenario from {@link CurrentScenario}. Rolled-back writes
 * are never counted; writes that happen outside a scenario execution are
 * delegated without being counted.
 */
@Component
@Primary
public class InstrumentedOrderRepository implements OrderRepository {

    private final JdbcOrderRepository delegate;
    private final DualWriteMetrics metrics;

    public InstrumentedOrderRepository(JdbcOrderRepository delegate, DualWriteMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public Order save(Order order) {
        Order saved = delegate.save(order);
        ScenarioId scenario = CurrentScenario.current();
        if (scenario != null) {
            CommittedWrites.record(() -> metrics.recordDbWrite(scenario));
        }
        return saved;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public List<Order> findByExperimentId(UUID experimentId) {
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
