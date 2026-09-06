package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;

import com.dualwrite.lab.kafka.EventPublisher;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;
import com.dualwrite.lab.scenario.shared.ScenarioContext;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import com.dualwrite.lab.scenario.shared.ScenarioPayloadFactory;
import com.dualwrite.lab.scenario.shared.ScenarioPort;

@Component
public class DualWriteBothOkScenario implements ScenarioPort {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final ScenarioPayloadFactory payloadFactory;

    public DualWriteBothOkScenario(
            OrderRepository orderRepository,
            EventPublisher eventPublisher,
            ScenarioPayloadFactory payloadFactory
    ) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.payloadFactory = payloadFactory;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.DUAL_WRITE_BOTH_OK;
    }

    @Override
    public void execute(ScenarioContext context) {
        Order order = Order.create(context.experimentId(), context.customerId(), context.total());
        orderRepository.save(order);
        eventPublisher.publish(payloadFactory.toEvent(order));
    }
}
