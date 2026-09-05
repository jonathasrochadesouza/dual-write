package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;

import com.dualwrite.lab.fault.FaultInjector;
import com.dualwrite.lab.fault.FaultPoint;
import com.dualwrite.lab.kafka.KafkaEventPublisher;
import com.dualwrite.lab.metrics.DualWriteMetrics;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;

@Component
public class DualWriteKafkaOnlyScenario implements ScenarioPort {

    private final OrderRepository orderRepository;
    private final KafkaEventPublisher eventPublisher;
    private final ScenarioPayloadFactory payloadFactory;
    private final FaultInjector faultInjector;
    private final DualWriteMetrics metrics;

    public DualWriteKafkaOnlyScenario(
            OrderRepository orderRepository,
            KafkaEventPublisher eventPublisher,
            ScenarioPayloadFactory payloadFactory,
            FaultInjector faultInjector,
            DualWriteMetrics metrics
    ) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.payloadFactory = payloadFactory;
        this.faultInjector = faultInjector;
        this.metrics = metrics;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.DUAL_WRITE_KAFKA_ONLY;
    }

    @Override
    public void execute(ScenarioContext context) {
        var sample = metrics.startTransactionTimer();
        try {
            Order order = Order.create(context.experimentId(), context.customerId(), context.total());
            eventPublisher.publish(payloadFactory.toEvent(order), id());
            metrics.recordKafkaPublish(id());

            faultInjector.maybeFail(FaultPoint.FAIL_DB_AFTER_PUBLISH, FaultPoint.FAIL_DB_AFTER_PUBLISH);
            orderRepository.save(order);
            metrics.recordDbWrite(id());
        } finally {
            metrics.stopTransactionTimer(sample, id());
        }
    }
}
