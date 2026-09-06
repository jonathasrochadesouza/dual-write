package com.dualwrite.lab.api;

import java.math.BigDecimal;

import com.dualwrite.lab.scenario.shared.ScenarioId;
import jakarta.validation.constraints.NotNull;

public record RunExperimentRequest(
        @NotNull ScenarioId scenario,
        String customerId,
        BigDecimal total
) {
}
