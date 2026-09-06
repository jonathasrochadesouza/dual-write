package com.dualwrite.lab.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes an order inside its own transaction, so the write is committed as
 * soon as {@link #save(Order)} returns. Scenarios that simulate a failure
 * after the database write rely on this boundary to observe the committed
 * state before the simulated outage happens.
 */
@Service
public class TransactionalOrderWriter {

    private final OrderRepository orderRepository;

    public TransactionalOrderWriter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }
}
