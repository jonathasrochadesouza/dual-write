package com.dualwrite.lab.outbox;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository {
    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findByExperimentId(UUID experimentId);

    long countByExperimentId(UUID experimentId);

    void deleteAll();
}
