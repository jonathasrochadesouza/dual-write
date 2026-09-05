package com.dualwrite.lab.report;

import org.springframework.stereotype.Component;

import com.dualwrite.lab.scenario.ScenarioId;

@Component
public class VerdictDeriver {

    public Verdict derive(ScenarioId scenarioId, ObservedState observed) {
        ExpectedOutcome expected = ExpectedOutcome.forScenario(scenarioId);
        boolean matches =
                expected.orderPresent() == observed.orderPresent()
                        && expected.outboxPresent() == observed.outboxPresent()
                        && expected.kafkaPresent() == observed.kafkaPresent();

        if (matches) {
            return expected.expectedVerdict();
        }

        if (!observed.orderPresent() && !observed.outboxPresent() && !observed.kafkaPresent()) {
            return Verdict.OPERACAO_PERDIDA;
        }

        if (observed.orderPresent() != observed.kafkaPresent()) {
            return Verdict.INCONSISTENTE;
        }

        if (scenarioId == ScenarioId.OUTBOX_COMMIT || scenarioId == ScenarioId.OUTBOX_ROLLBACK) {
            if (observed.orderPresent() == observed.outboxPresent()) {
                return Verdict.ATOMICO;
            }
            return Verdict.INCONSISTENTE;
        }

        return Verdict.INCONSISTENTE;
    }
}
