package com.dualwrite.lab.scenario;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dualwrite.lab.order.Order;
import com.dualwrite.lab.order.OrderRepository;

@Service
public class OrderPersistence {

    private final OrderRepository orderRepository;

    public OrderPersistence(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }
}
