package com.wei.orchestrator.order.domain.service;

import com.wei.orchestrator.order.domain.exception.OrderAlreadyExistsException;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderDomainService {

    private final OrderRepository orderRepository;

    public OrderDomainService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void validateOrderCreation(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        if (orderRepository.findById(orderId).isPresent()) {
            throw new OrderAlreadyExistsException("Order with ID " + orderId + " already exists");
        }
    }
}
