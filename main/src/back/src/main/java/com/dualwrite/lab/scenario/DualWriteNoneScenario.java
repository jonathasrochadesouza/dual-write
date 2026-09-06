package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dualwrite.lab.fault.FaultInjector;
import com.dualwrite.lab.fault.FaultPoint;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;
import com.dualwrite.lab.scenario.shared.ScenarioContext;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import com.dualwrite.lab.scenario.shared.ScenarioPort;

@Component
public class DualWriteNoneScenario implements ScenarioPort {

    private final OrderRepository orderRepository;
    private final FaultInjector faultInjector;

    public DualWriteNoneScenario(
            OrderRepository orderRepository,
            FaultInjector faultInjector
    ) {
        this.orderRepository = orderRepository;
        this.faultInjector = faultInjector;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.DUAL_WRITE_NONE;
    }

    @Override
    @Transactional
    public void execute(ScenarioContext context) {
        Order order = Order.create(context.experimentId(), context.customerId(), context.total());
        orderRepository.save(order);
        faultInjector.fail(FaultPoint.FAIL_BEFORE_COMMIT);
    }
}
