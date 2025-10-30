package com.wei.orchestrator.order.domain.repository;

import com.wei.orchestrator.order.domain.model.Order;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(String orderId);

    void deleteById(String orderId);
}
