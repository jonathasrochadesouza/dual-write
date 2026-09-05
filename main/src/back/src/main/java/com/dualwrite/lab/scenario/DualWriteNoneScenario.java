package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dualwrite.lab.fault.FaultInjector;
import com.dualwrite.lab.fault.FaultPoint;
import com.dualwrite.lab.metrics.DualWriteMetrics;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;

@Component
public class DualWriteNoneScenario implements ScenarioPort {

    private final OrderRepository orderRepository;
    private final FaultInjector faultInjector;
    private final DualWriteMetrics metrics;

    public DualWriteNoneScenario(
            OrderRepository orderRepository,
            FaultInjector faultInjector,
            DualWriteMetrics metrics
    ) {
        this.orderRepository = orderRepository;
        this.faultInjector = faultInjector;
        this.metrics = metrics;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.DUAL_WRITE_NONE;
    }

    @Override
    @Transactional
    public void execute(ScenarioContext context) {
        var sample = metrics.startTransactionTimer();
        try {
            Order order = Order.create(context.experimentId(), context.customerId(), context.total());
            orderRepository.save(order);
            faultInjector.maybeFail(FaultPoint.FAIL_BEFORE_COMMIT, FaultPoint.FAIL_BEFORE_COMMIT);
        } catch (RuntimeException ex) {
            metrics.recordDbRollback(id());
            throw ex;
        } finally {
            metrics.stopTransactionTimer(sample, id());
        }
    }
}
