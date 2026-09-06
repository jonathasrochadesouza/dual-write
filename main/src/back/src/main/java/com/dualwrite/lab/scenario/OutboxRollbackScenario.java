package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dualwrite.lab.fault.FaultInjector;
import com.dualwrite.lab.fault.FaultPoint;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;
import com.dualwrite.lab.outbox.OutboxEvent;
import com.dualwrite.lab.outbox.OutboxRepository;
import com.dualwrite.lab.scenario.shared.ScenarioContext;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import com.dualwrite.lab.scenario.shared.ScenarioPayloadFactory;
import com.dualwrite.lab.scenario.shared.ScenarioPort;

@Component
public class OutboxRollbackScenario implements ScenarioPort {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ScenarioPayloadFactory payloadFactory;
    private final FaultInjector faultInjector;

    public OutboxRollbackScenario(
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            ScenarioPayloadFactory payloadFactory,
            FaultInjector faultInjector
    ) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.payloadFactory = payloadFactory;
        this.faultInjector = faultInjector;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.OUTBOX_ROLLBACK;
    }

    @Override
    @Transactional
    public void execute(ScenarioContext context) {
        Order order = Order.create(context.experimentId(), context.customerId(), context.total());
        orderRepository.save(order);

        OutboxEvent event = OutboxEvent.pending(
                context.experimentId(),
                order.id(),
                "OrderCreated",
                payloadFactory.toJson(payloadFactory.toEvent(order))
        );
        outboxRepository.save(event);

        faultInjector.fail(FaultPoint.FAIL_BEFORE_COMMIT);
    }
}
