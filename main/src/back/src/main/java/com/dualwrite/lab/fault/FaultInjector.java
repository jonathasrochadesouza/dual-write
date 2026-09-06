package com.dualwrite.lab.fault;

import org.springframework.stereotype.Component;

/**
 * Throws a {@link SimulatedOutageException} at the requested point of the
 * scenario, simulating an outage of the dependency being written to.
 */
@Component
public class FaultInjector {

    public void fail(FaultPoint point) {
        throw new SimulatedOutageException(point);
    }
}
