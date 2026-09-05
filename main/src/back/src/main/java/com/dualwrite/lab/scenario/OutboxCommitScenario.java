package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dualwrite.lab.metrics.DualWriteMetrics;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;
import com.dualwrite.lab.outbox.OutboxEvent;
import com.dualwrite.lab.outbox.OutboxRepository;

@Component
public class OutboxCommitScenario implements ScenarioPort {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ScenarioPayloadFactory payloadFactory;
    private final DualWriteMetrics metrics;

    public OutboxCommitScenario(
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            ScenarioPayloadFactory payloadFactory,
            DualWriteMetrics metrics
    ) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.payloadFactory = payloadFactory;
        this.metrics = metrics;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.OUTBOX_COMMIT;
    }

    @Override
    @Transactional
    public void execute(ScenarioContext context) {
        var sample = metrics.startTransactionTimer();
        try {
            Order order = Order.create(context.experimentId(), context.customerId(), context.total());
            orderRepository.save(order);
            metrics.recordDbWrite(id());

            OutboxEvent event = OutboxEvent.pending(
                    context.experimentId(),
                    order.id(),
                    "OrderCreated",
                    payloadFactory.toJson(payloadFactory.toEvent(order))
            );
            outboxRepository.save(event);
            metrics.recordOutboxCommitted(id());
        } finally {
            metrics.stopTransactionTimer(sample, id());
        }
    }
}
