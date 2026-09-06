package com.dualwrite.lab.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.outbox.OutboxEvent;
import com.dualwrite.lab.scenario.shared.ScenarioId;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VerdictDeriverTest {

    private final VerdictDeriver deriver = new VerdictDeriver();

    @Test
    void c3IsInconsistente() {
        ObservedState observed = new ObservedState(
                List.of(sampleOrder()),
                List.of(),
                List.of()
        );
        assertEquals(Verdict.INCONSISTENTE, deriver.derive(ScenarioId.DUAL_WRITE_DB_ONLY, observed));
    }

    @Test
    void c2IsOperacaoPerdida() {
        ObservedState observed = new ObservedState(List.of(), List.of(), List.of());
        assertEquals(Verdict.OPERACAO_PERDIDA, deriver.derive(ScenarioId.DUAL_WRITE_NONE, observed));
    }

    @Test
    void c5IsAtomicoWithEmptyKafka() {
        ObservedState observed = new ObservedState(
                List.of(sampleOrder()),
                List.of(sampleOutbox()),
                List.of()
        );
        assertEquals(Verdict.ATOMICO, deriver.derive(ScenarioId.OUTBOX_COMMIT, observed));
    }

    @Test
    void c1IsAtomicoWhenBothPresent() {
        ObservedState observed = new ObservedState(
                List.of(sampleOrder()),
                List.of(),
                List.of("{\"experimentId\":\"" + UUID.randomUUID() + "\"}")
        );
        assertEquals(Verdict.ATOMICO, deriver.derive(ScenarioId.DUAL_WRITE_BOTH_OK, observed));
    }

    private static Order sampleOrder() {
        return new Order(UUID.randomUUID(), UUID.randomUUID(), "c-1", new BigDecimal("10.00"), "CREATED", Instant.now());
    }

    private static OutboxEvent sampleOutbox() {
        return new OutboxEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "OrderCreated",
                "{}",
                "PENDING",
                Instant.now()
        );
    }
}
