package com.dualwrite.lab.scenario.shared;

public interface ScenarioPort {
    ScenarioId id();

    void execute(ScenarioContext context);
}
