package com.dualwrite.lab.report;

import java.time.Instant;
import java.util.UUID;

import com.dualwrite.lab.scenario.shared.ScenarioId;

public record ExperimentReport(
        UUID experimentId,
        ScenarioId scenario,
        Instant executedAt,
        ExpectedOutcome expected,
        ObservedState observed,
        Verdict verdict,
        String message
) {
}
