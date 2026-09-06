package com.dualwrite.lab.metrics;

import com.dualwrite.lab.scenario.shared.ScenarioId;

/**
 * Propagates the scenario currently being executed to the instrumented
 * collaborators, so they can tag their metrics with the right scenario
 * without the scenario code passing identifiers around.
 */
public final class CurrentScenario {

    private static final ThreadLocal<ScenarioId> CURRENT = new ThreadLocal<>();

    private CurrentScenario() {
    }

    public static void set(ScenarioId scenarioId) {
        CURRENT.set(scenarioId);
    }

    public static ScenarioId current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
