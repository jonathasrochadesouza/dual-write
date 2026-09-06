package com.dualwrite.lab.metrics;

import com.dualwrite.lab.scenario.shared.ScenarioContext;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import com.dualwrite.lab.scenario.shared.ScenarioPort;

/**
 * Wraps a scenario with observability: measures the scenario duration
 * (including the commit of the transactional proxy it wraps) and makes the
 * scenario available to instrumented collaborators through
 * {@link CurrentScenario}, keeping metric code out of the scenario bodies.
 */
public class InstrumentedScenario implements ScenarioPort {

    private final ScenarioPort delegate;
    private final DualWriteMetrics metrics;

    public InstrumentedScenario(ScenarioPort delegate, DualWriteMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public ScenarioId id() {
        return delegate.id();
    }

    @Override
    public void execute(ScenarioContext context) {
        CurrentScenario.set(id());
        var sample = metrics.startScenarioTimer();
        try {
            delegate.execute(context);
        } finally {
            metrics.stopScenarioTimer(sample, id());
            CurrentScenario.clear();
        }
    }
}
