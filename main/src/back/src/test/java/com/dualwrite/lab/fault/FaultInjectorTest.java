package com.dualwrite.lab.fault;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FaultInjectorTest {

    private final FaultInjector faultInjector = new FaultInjector();

    @Test
    void failsAtRequestedPoint() {
        SimulatedOutageException ex = assertThrows(
                SimulatedOutageException.class,
                () -> faultInjector.fail(FaultPoint.FAIL_BEFORE_COMMIT)
        );
        assertEquals(FaultPoint.FAIL_BEFORE_COMMIT, ex.faultPoint());
    }

    @Test
    void failsAtPublishAfterCommit() {
        SimulatedOutageException ex = assertThrows(
                SimulatedOutageException.class,
                () -> faultInjector.fail(FaultPoint.FAIL_PUBLISH_AFTER_COMMIT)
        );
        assertEquals(FaultPoint.FAIL_PUBLISH_AFTER_COMMIT, ex.faultPoint());
    }

    @Test
    void failsAtDbAfterPublish() {
        SimulatedOutageException ex = assertThrows(
                SimulatedOutageException.class,
                () -> faultInjector.fail(FaultPoint.FAIL_DB_AFTER_PUBLISH)
        );
        assertEquals(FaultPoint.FAIL_DB_AFTER_PUBLISH, ex.faultPoint());
    }
}
