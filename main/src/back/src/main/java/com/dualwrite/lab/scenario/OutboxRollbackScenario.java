package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dualwrite.lab.fault.FaultInjector;
import com.dualwrite.lab.fault.FaultPoint;
import com.dualwrite.lab.metrics.DualWriteMetrics;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;
import com.dualwrite.lab.outbox.OutboxEvent;
import com.dualwrite.lab.outbox.OutboxRepository;

@Component
public class OutboxRollbackScenario implements ScenarioPort {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ScenarioPayloadFactory payloadFactory;
    private final FaultInjector faultInjector;
    private final DualWriteMetrics metrics;

    public OutboxRollbackScenario(
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            ScenarioPayloadFactory payloadFactory,
            FaultInjector faultInjector,
            DualWriteMetrics metrics
    ) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.payloadFactory = payloadFactory;
        this.faultInjector = faultInjector;
        this.metrics = metrics;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.OUTBOX_ROLLBACK;
    }

    @Override
    @Transactional
    public void execute(ScenarioContext context) {
        var sample = metrics.startTransactionTimer();
        try {
            Order order = Order.create(context.experimentId(), context.customerId(), context.total());
            orderRepository.save(order);

            OutboxEvent event = OutboxEvent.pending(
                    context.experimentId(),
                    order.id(),
                    "OrderCreated",
                    payloadFactory.toJson(payloadFactory.toEvent(order))
            );
            outboxRepository.save(event);

            faultInjector.maybeFail(FaultPoint.FAIL_BEFORE_COMMIT, FaultPoint.FAIL_BEFORE_COMMIT);
        } catch (RuntimeException ex) {
            metrics.recordDbRollback(id());
            throw ex;
        } finally {
            metrics.stopTransactionTimer(sample, id());
        }
    }
}
