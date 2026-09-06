package com.dualwrite.lab.scenario;

public interface ScenarioPort {
    ScenarioId id();

    void execute(ScenarioContext context);
}
