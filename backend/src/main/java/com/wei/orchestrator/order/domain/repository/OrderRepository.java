package com.wei.orchestrator.order.domain.repository;

import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(String orderId);

    void deleteById(String orderId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findScheduledOrdersReadyForFulfillment(LocalDateTime currentTime);
}
