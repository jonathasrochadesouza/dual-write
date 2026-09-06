package com.dualwrite.lab.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dualwrite.lab.fault.FaultPoint;
import com.dualwrite.lab.fault.SimulatedOutageException;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InstrumentedFaultInjectorTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final DualWriteMetrics metrics = new DualWriteMetrics(registry);
    private final InstrumentedFaultInjector faultInjector = new InstrumentedFaultInjector(metrics);

    @AfterEach
    void clearScenarioContext() {
        CurrentScenario.clear();
    }

    @Test
    void countsPublishOutageAsKafkaFailure() {
        CurrentScenario.set(ScenarioId.DUAL_WRITE_DB_ONLY);

        assertThrows(SimulatedOutageException.class, () -> faultInjector.fail(FaultPoint.FAIL_PUBLISH_AFTER_COMMIT));

        assertEquals(1.0, registry.get("dualwrite.failures.kafka.total")
                .tag("scenario", ScenarioId.DUAL_WRITE_DB_ONLY.name())
                .counter()
                .count());
    }

    @Test
    void countsOutageBeforeCommitAsDatabaseFailure() {
        CurrentScenario.set(ScenarioId.DUAL_WRITE_NONE);

        assertThrows(SimulatedOutageException.class, () -> faultInjector.fail(FaultPoint.FAIL_BEFORE_COMMIT));

        assertEquals(1.0, registry.get("dualwrite.failures.db.total")
                .tag("scenario", ScenarioId.DUAL_WRITE_NONE.name())
                .counter()
                .count());
    }

    @Test
    void countsDatabaseOutageAfterPublishAsDatabaseFailure() {
        CurrentScenario.set(ScenarioId.DUAL_WRITE_KAFKA_ONLY);

        assertThrows(SimulatedOutageException.class, () -> faultInjector.fail(FaultPoint.FAIL_DB_AFTER_PUBLISH));

        assertEquals(1.0, registry.get("dualwrite.failures.db.total")
                .tag("scenario", ScenarioId.DUAL_WRITE_KAFKA_ONLY.name())
                .counter()
                .count());
    }

    @Test
    void rethrowsWithoutCountingWhenNoScenarioIsActive() {
        assertThrows(SimulatedOutageException.class, () -> faultInjector.fail(FaultPoint.FAIL_BEFORE_COMMIT));

        assertEquals(0, registry.find("dualwrite.failures.db.total").counters().size());
        assertEquals(0, registry.find("dualwrite.failures.kafka.total").counters().size());
    }
}
