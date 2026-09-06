package com.dualwrite.lab.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dualwrite.lab.scenario.shared.ScenarioContext;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import com.dualwrite.lab.scenario.shared.ScenarioPort;
import com.dualwrite.lab.scenario.shared.ScenarioRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScenarioRegistryTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final DualWriteMetrics metrics = new DualWriteMetrics(registry);

    @Test
    void instrumentsEveryRegisteredScenario() {
        ScenarioRegistry scenarioRegistry = new ScenarioRegistry(
                List.of(new StubScenario(ScenarioId.DUAL_WRITE_BOTH_OK), new StubScenario(ScenarioId.OUTBOX_COMMIT)),
                metrics
        );

        for (ScenarioId scenarioId : ScenarioId.values()) {
            if (scenarioId == ScenarioId.DUAL_WRITE_BOTH_OK || scenarioId == ScenarioId.OUTBOX_COMMIT) {
                assertEquals(InstrumentedScenario.class, scenarioRegistry.require(scenarioId).getClass());
            } else {
                var ex = assertThrows(IllegalArgumentException.class, () -> scenarioRegistry.require(scenarioId));
                assertEquals("Unsupported scenario: " + scenarioId, ex.getMessage());
            }
        }
    }

    @Test
    void delegatesExecutionToInstrumentedScenario() {
        ScenarioRegistry scenarioRegistry = new ScenarioRegistry(List.of(new StubScenario(ScenarioId.DUAL_WRITE_NONE)), metrics);

        scenarioRegistry.require(ScenarioId.DUAL_WRITE_NONE).execute(new ScenarioContext(
                null, ScenarioId.DUAL_WRITE_NONE, "customer-42", null));

        assertEquals(1L, registry.get("dualwrite.scenario.duration")
                .tag("scenario", ScenarioId.DUAL_WRITE_NONE.name())
                .timer()
                .count());
    }

    private record StubScenario(ScenarioId scenarioId) implements ScenarioPort {

        @Override
        public ScenarioId id() {
            return scenarioId;
        }

        @Override
        public void execute(ScenarioContext context) {
        }
    }
}
