package com.dualwrite.lab.scenario;

public enum ScenarioId {
    DUAL_WRITE_BOTH_OK,
    DUAL_WRITE_NONE,
    DUAL_WRITE_DB_ONLY,
    DUAL_WRITE_KAFKA_ONLY,
    OUTBOX_COMMIT,
    OUTBOX_ROLLBACK
}
