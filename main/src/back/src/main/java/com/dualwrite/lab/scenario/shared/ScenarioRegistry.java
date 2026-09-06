package com.dualwrite.lab.scenario;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ScenarioRegistry {

    private final Map<ScenarioId, ScenarioPort> scenarios = new EnumMap<>(ScenarioId.class);

    public ScenarioRegistry(List<ScenarioPort> scenarioPorts) {
        for (ScenarioPort port : scenarioPorts) {
            scenarios.put(port.id(), port);
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
