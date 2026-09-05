package com.dualwrite.lab.fault;

public class SimulatedOutageException extends RuntimeException {

    private final FaultPoint faultPoint;

    public SimulatedOutageException(FaultPoint faultPoint) {
        super("Simulated service outage at " + faultPoint);
        this.faultPoint = faultPoint;
    }

    public FaultPoint faultPoint() {
        return faultPoint;
    }
}
