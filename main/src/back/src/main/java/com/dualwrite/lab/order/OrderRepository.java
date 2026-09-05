package com.dualwrite.lab.order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(UUID id);

    List<Order> findByExperimentId(UUID experimentId);

    long countByExperimentId(UUID experimentId);

    void deleteAll();
}
