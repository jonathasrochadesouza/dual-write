package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Component;

import com.dualwrite.lab.fault.FaultInjector;
import com.dualwrite.lab.fault.FaultPoint;
import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.TransactionalOrderWriter;
import com.dualwrite.lab.scenario.shared.ScenarioContext;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import com.dualwrite.lab.scenario.shared.ScenarioPort;

@Component
public class DualWriteDbOnlyScenario implements ScenarioPort {

    private final TransactionalOrderWriter orderWriter;
    private final FaultInjector faultInjector;

    public DualWriteDbOnlyScenario(
            TransactionalOrderWriter orderWriter,
            FaultInjector faultInjector
    ) {
        this.orderWriter = orderWriter;
        this.faultInjector = faultInjector;
    }

    @Override
    public ScenarioId id() {
        return ScenarioId.DUAL_WRITE_DB_ONLY;
    }

    @Override
    public void execute(ScenarioContext context) {
        Order order = Order.create(context.experimentId(), context.customerId(), context.total());
        orderWriter.save(order);
        faultInjector.fail(FaultPoint.FAIL_PUBLISH_AFTER_COMMIT);
    }
}
