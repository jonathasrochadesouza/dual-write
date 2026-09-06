package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;

import com.dualwrite.lab.fault.FaultInjector;
import com.dualwrite.lab.fault.FaultPoint;
import com.dualwrite.lab.kafka.EventPublisher;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;
import com.dualwrite.lab.scenario.shared.ScenarioContext;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import com.dualwrite.lab.scenario.shared.ScenarioPayloadFactory;
import com.dualwrite.lab.scenario.shared.ScenarioPort;

@Component
public class DualWriteKafkaOnlyScenario implements ScenarioPort {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final ScenarioPayloadFactory payloadFactory;
    private final FaultInjector faultInjector;

    public DualWriteKafkaOnlyScenario(
            OrderRepository orderRepository,
            EventPublisher eventPublisher,
            ScenarioPayloadFactory payloadFactory,
            FaultInjector faultInjector
    ) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.payloadFactory = payloadFactory;
        this.faultInjector = faultInjector;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.DUAL_WRITE_KAFKA_ONLY;
    }

    @Override
    public void execute(ScenarioContext context) {
        Order order = Order.create(context.experimentId(), context.customerId(), context.total());
        eventPublisher.publish(payloadFactory.toEvent(order));
        faultInjector.fail(FaultPoint.FAIL_DB_AFTER_PUBLISH);
        orderRepository.save(order);
    }
}
