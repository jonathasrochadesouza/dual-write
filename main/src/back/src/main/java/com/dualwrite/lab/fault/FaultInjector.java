package com.dualwrite.lab.fault;

import org.springframework.stereotype.Component;

@Component
public class FaultInjector {

    public void maybeFail(FaultPoint activePoint, FaultPoint currentPoint) {
        if (activePoint == currentPoint) {
            throw new SimulatedOutageException(currentPoint);
        }
    }
}
