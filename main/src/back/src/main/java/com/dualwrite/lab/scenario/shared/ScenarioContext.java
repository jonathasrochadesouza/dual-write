package com.dualwrite.lab.scenario;

import java.math.BigDecimal;
import java.util.UUID;

public record ScenarioContext(
        UUID experimentId,
        ScenarioId scenarioId,
        String customerId,
        BigDecimal total
) {
}
