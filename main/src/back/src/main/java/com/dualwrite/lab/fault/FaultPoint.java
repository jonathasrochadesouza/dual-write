package com.dualwrite.lab.fault;

public enum FaultPoint {
    FAIL_BEFORE_COMMIT,
    FAIL_PUBLISH_AFTER_COMMIT,
    FAIL_DB_AFTER_PUBLISH
}
