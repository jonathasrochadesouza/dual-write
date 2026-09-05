package com.dualwrite.lab.fault;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FaultInjectorTest {

    private final FaultInjector faultInjector = new FaultInjector();

    @Test
    void failsAtMatchingPoint() {
        SimulatedOutageException ex = assertThrows(
                SimulatedOutageException.class,
                () -> faultInjector.maybeFail(FaultPoint.FAIL_BEFORE_COMMIT, FaultPoint.FAIL_BEFORE_COMMIT)
        );
        assertEquals(FaultPoint.FAIL_BEFORE_COMMIT, ex.faultPoint());
    }

    @Test
    void failsAtPublishAfterCommit() {
        SimulatedOutageException ex = assertThrows(
                SimulatedOutageException.class,
                () -> faultInjector.maybeFail(FaultPoint.FAIL_PUBLISH_AFTER_COMMIT, FaultPoint.FAIL_PUBLISH_AFTER_COMMIT)
        );
        assertEquals(FaultPoint.FAIL_PUBLISH_AFTER_COMMIT, ex.faultPoint());
    }

    @Test
    void failsAtDbAfterPublish() {
        SimulatedOutageException ex = assertThrows(
                SimulatedOutageException.class,
                () -> faultInjector.maybeFail(FaultPoint.FAIL_DB_AFTER_PUBLISH, FaultPoint.FAIL_DB_AFTER_PUBLISH)
        );
        assertEquals(FaultPoint.FAIL_DB_AFTER_PUBLISH, ex.faultPoint());
    }

    @Test
    void doesNothingWhenPointsDiffer() {
        assertDoesNotThrow(() -> faultInjector.maybeFail(FaultPoint.FAIL_BEFORE_COMMIT, FaultPoint.FAIL_DB_AFTER_PUBLISH));
    }
}
