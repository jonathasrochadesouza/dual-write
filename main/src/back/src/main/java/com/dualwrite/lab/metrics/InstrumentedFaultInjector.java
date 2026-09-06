package com.dualwrite.lab.metrics;

import com.dualwrite.lab.fault.FaultInjector;
import com.dualwrite.lab.fault.FaultPoint;
import com.dualwrite.lab.fault.SimulatedOutageException;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Counts the failures injected through {@link FaultInjector}, mapped from the
 * fault point: a publish outage is a Kafka failure, every other outage (a
 * database write that did not survive) is a database failure. Failures thrown
 * outside a scenario execution are rethrown without being counted.
 */
@Component
@Primary
public class InstrumentedFaultInjector extends FaultInjector {

    private final DualWriteMetrics metrics;

    public InstrumentedFaultInjector(DualWriteMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void fail(FaultPoint point) {
        try {
            super.fail(point);
        } catch (SimulatedOutageException ex) {
            recordFailure(ex.faultPoint());
            throw ex;
        }
    }

    private void recordFailure(FaultPoint point) {
        ScenarioId scenario = CurrentScenario.current();
        if (scenario == null) {
            return;
        }
        if (point == FaultPoint.FAIL_PUBLISH_AFTER_COMMIT) {
            metrics.recordKafkaPublishFailure(scenario);
        } else {
            metrics.recordDatabaseWriteFailure(scenario);
        }
    }
}
