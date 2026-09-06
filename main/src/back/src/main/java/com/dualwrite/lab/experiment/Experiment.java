package com.dualwrite.lab.experiment;

import java.time.Instant;
import java.util.UUID;

import com.dualwrite.lab.report.Verdict;
import com.dualwrite.lab.scenario.shared.ScenarioId;

public record Experiment(
        UUID id,
        ScenarioId scenario,
        Instant executedAt,
        Verdict verdict
) {
}
