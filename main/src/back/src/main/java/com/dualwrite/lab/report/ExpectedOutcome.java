package com.dualwrite.lab.report;

import com.dualwrite.lab.scenario.ScenarioId;

public record ExpectedOutcome(
        boolean orderPresent,
        boolean outboxPresent,
        boolean kafkaPresent,
        boolean kafkaEmptyExpected,
        Verdict expectedVerdict,
        String description
) {
    public static ExpectedOutcome forScenario(ScenarioId scenarioId) {
        return switch (scenarioId) {
            case DUAL_WRITE_BOTH_OK -> new ExpectedOutcome(
                    true, false, true, false, Verdict.ATOMICO,
                    "Pedido no banco e evento no Kafka (correto por acaso, sem atomicidade)"
            );
            case DUAL_WRITE_NONE -> new ExpectedOutcome(
                    false, false, false, true, Verdict.OPERACAO_PERDIDA,
                    "Nada no banco nem no Kafka — operação perdida"
            );
            case DUAL_WRITE_DB_ONLY -> new ExpectedOutcome(
                    true, false, false, true, Verdict.INCONSISTENTE,
                    "Pedido fantasma no banco; evento ausente no Kafka (outage de publish)"
            );
            case DUAL_WRITE_KAFKA_ONLY -> new ExpectedOutcome(
                    false, false, true, false, Verdict.INCONSISTENTE,
                    "Evento fantasma no Kafka; pedido ausente no banco"
            );
            case OUTBOX_COMMIT -> new ExpectedOutcome(
                    true, true, false, true, Verdict.ATOMICO,
                    "orders + outbox_events commitados; Kafka vazio (sem relay)"
            );
            case OUTBOX_ROLLBACK -> new ExpectedOutcome(
                    false, false, false, true, Verdict.ATOMICO,
                    "Rollback atômico das duas tabelas; Kafka vazio"
            );
        };
    }
}
