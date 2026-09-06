package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;

import com.dualwrite.lab.fault.FaultInjector;
import com.dualwrite.lab.fault.FaultPoint;
import com.dualwrite.lab.fault.SimulatedOutageException;
import com.dualwrite.lab.metrics.DualWriteMetrics;
import com.dualwrite.lab.order.Order;

@Component
public class DualWriteDbOnlyScenario implements ScenarioPort {

    private final OrderPersistence orderPersistence;
    private final FaultInjector faultInjector;
    private final DualWriteMetrics metrics;

    public DualWriteDbOnlyScenario(
        OrderPersistence orderPersistence,
        FaultInjector faultInjector,
        DualWriteMetrics metrics
    ) {
        this.orderPersistence = orderPersistence;
        this.faultInjector = faultInjector;
        this.metrics = metrics;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.DUAL_WRITE_DB_ONLY;
    }

    @Override
    public void execute(ScenarioContext context) {
        var sample = metrics.startTransactionTimer();
        try {
            Order order = Order.create(context.experimentId(), context.customerId(), context.total());
            orderPersistence.save(order);
            metrics.recordDbWrite(id());
            try {
                faultInjector.maybeFail(FaultPoint.FAIL_PUBLISH_AFTER_COMMIT, FaultPoint.FAIL_PUBLISH_AFTER_COMMIT);
            } catch (SimulatedOutageException ex) {
                metrics.recordKafkaPublishFailure(id());
                throw ex;
            }
        } finally {
            metrics.stopTransactionTimer(sample, id());
        }
    }
}
