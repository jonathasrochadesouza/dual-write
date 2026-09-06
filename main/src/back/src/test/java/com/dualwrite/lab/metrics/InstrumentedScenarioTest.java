package com.dualwrite.lab.metrics;

import java.math.BigDecimal;
import java.util.UUID;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.dualwrite.lab.scenario.shared.ScenarioContext;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import com.dualwrite.lab.scenario.shared.ScenarioPort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstrumentedScenarioTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final DualWriteMetrics metrics = new DualWriteMetrics(registry);

    @AfterEach
    void clearScenarioContext() {
        CurrentScenario.clear();
    }

    @Test
    void exposesScenarioToCollaboratorsDuringExecution() {
        ScenarioPort scenario = instrumentedScenarioFor(ScenarioId.DUAL_WRITE_BOTH_OK, () ->
                assertEquals(ScenarioId.DUAL_WRITE_BOTH_OK, CurrentScenario.current()));

        scenario.execute(context());
    }

    @Test
    void recordsScenarioDurationWithScenarioTag() {
        ScenarioPort scenario = instrumentedScenarioFor(ScenarioId.OUTBOX_COMMIT, () -> { });

        scenario.execute(context());

        var timer = registry.get("dualwrite.scenario.duration")
                .tag("scenario", ScenarioId.OUTBOX_COMMIT.name())
                .timer();
        assertEquals(1L, timer.count());
    }

    @Test
    void clearsScenarioAfterExecution() {
        ScenarioPort scenario = instrumentedScenarioFor(ScenarioId.DUAL_WRITE_NONE, () -> { });

        scenario.execute(context());

        assertNull(CurrentScenario.current());
    }

    @Test
    void clearsScenarioWhenExecutionFails() {
        ScenarioPort scenario = instrumentedScenarioFor(ScenarioId.DUAL_WRITE_DB_ONLY, () -> {
            throw new IllegalStateException("boom");
        });

        assertThrows(IllegalStateException.class, () -> scenario.execute(context()));

        assertNull(CurrentScenario.current());
        assertEquals(1L, registry.get("dualwrite.scenario.duration")
                .tag("scenario", ScenarioId.DUAL_WRITE_DB_ONLY.name())
                .timer()
                .count());
    }

    @Test
    void doesNotLeakScenarioBetweenConsecutiveExecutions() {
        ScenarioPort first = instrumentedScenarioFor(ScenarioId.DUAL_WRITE_BOTH_OK, () -> { });
        ScenarioPort second = instrumentedScenarioFor(ScenarioId.OUTBOX_ROLLBACK, () -> { });

        first.execute(context());
        second.execute(context());

        assertNull(CurrentScenario.current());
        assertEquals(1L, registry.get("dualwrite.scenario.duration")
                .tag("scenario", ScenarioId.DUAL_WRITE_BOTH_OK.name())
                .timer()
                .count());
        assertEquals(1L, registry.get("dualwrite.scenario.duration")
                .tag("scenario", ScenarioId.OUTBOX_ROLLBACK.name())
                .timer()
                .count());
    }

    @Test
    void exposesDelegateId() {
        ScenarioPort scenario = instrumentedScenarioFor(ScenarioId.OUTBOX_COMMIT, () -> { });

        assertSame(ScenarioId.OUTBOX_COMMIT, scenario.id());
    }

    private InstrumentedScenario instrumentedScenarioFor(ScenarioId scenarioId, Runnable body) {
        return new InstrumentedScenario(new StubScenario(scenarioId, body), metrics);
    }

    private ScenarioContext context() {
        return new ScenarioContext(UUID.randomUUID(), ScenarioId.DUAL_WRITE_BOTH_OK, "customer-42", new BigDecimal("199.90"));
    }

    private record StubScenario(ScenarioId scenarioId, Runnable body) implements ScenarioPort {

        @Override
        public ScenarioId id() {
            return scenarioId;
        }

        @Override
        public void execute(ScenarioContext context) {
            body.run();
        }
    }
}
