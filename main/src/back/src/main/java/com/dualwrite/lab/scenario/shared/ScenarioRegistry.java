package com.dualwrite.lab.scenario.shared;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.dualwrite.lab.metrics.DualWriteMetrics;
import com.dualwrite.lab.metrics.InstrumentedScenario;

@Component
public class ScenarioRegistry {

    private final Map<ScenarioId, ScenarioPort> scenarios = new EnumMap<>(ScenarioId.class);

    public ScenarioRegistry(List<ScenarioPort> scenarioPorts, DualWriteMetrics metrics) {
        for (ScenarioPort port : scenarioPorts) {
            scenarios.put(port.id(), new InstrumentedScenario(port, metrics));
        }
    }

    public ScenarioPort require(ScenarioId scenarioId) {
        ScenarioPort port = scenarios.get(scenarioId);
        if (port == null) {
            throw new IllegalArgumentException("Unsupported scenario: " + scenarioId);
        }
        return port;
    }
}
