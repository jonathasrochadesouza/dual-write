package com.dualwrite.lab.metrics;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Records metrics only when the transaction surrounding the write commits,
 * so rolled-back work is never counted. Outside a transaction there is
 * nothing to wait for and the recording happens immediately.
 */
final class CommittedWrites {

    private CommittedWrites() {
    }

    static void record(Runnable recording) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    recording.run();
                }
            });
        } else {
            recording.run();
        }
    }
}
