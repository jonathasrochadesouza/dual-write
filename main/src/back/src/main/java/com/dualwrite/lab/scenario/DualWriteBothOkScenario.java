package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;

import com.dualwrite.lab.kafka.EventPublisher;
import com.dualwrite.lab.kafka.KafkaEventPublisher;
import com.dualwrite.lab.metrics.DualWriteMetrics;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;

@Component
public class DualWriteBothOkScenario implements ScenarioPort {

    private final OrderRepository orderRepository;
    private final KafkaEventPublisher eventPublisher;
    private final ScenarioPayloadFactory payloadFactory;
    private final DualWriteMetrics metrics;

    public DualWriteBothOkScenario(
            OrderRepository orderRepository,
            KafkaEventPublisher eventPublisher,
            ScenarioPayloadFactory payloadFactory,
            DualWriteMetrics metrics
    ) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.payloadFactory = payloadFactory;
        this.metrics = metrics;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.DUAL_WRITE_BOTH_OK;
    }

    @Override
    public void execute(ScenarioContext context) {
        var sample = metrics.startTransactionTimer();
        try {
            Order order = Order.create(context.experimentId(), context.customerId(), context.total());
            orderRepository.save(order);
            metrics.recordDbWrite(id());

            eventPublisher.publish(payloadFactory.toEvent(order), id());
            metrics.recordKafkaPublish(id());
        } finally {
            metrics.stopTransactionTimer(sample, id());
        }
    }
}
