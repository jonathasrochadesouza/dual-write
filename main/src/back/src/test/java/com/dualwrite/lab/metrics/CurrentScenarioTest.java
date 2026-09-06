package com.dualwrite.lab.metrics;

import org.junit.jupiter.api.Test;

import com.dualwrite.lab.scenario.shared.ScenarioId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CurrentScenarioTest {

    @Test
    void exposesSetScenario() {
        CurrentScenario.set(ScenarioId.OUTBOX_COMMIT);
        try {
            assertEquals(ScenarioId.OUTBOX_COMMIT, CurrentScenario.current());
        } finally {
            CurrentScenario.clear();
        }
    }

    @Test
    void clearsScenario() {
        CurrentScenario.set(ScenarioId.DUAL_WRITE_BOTH_OK);
        CurrentScenario.clear();
        assertNull(CurrentScenario.current());
    }
}
