package com.wei.orchestrator.order.infrastructure.repository;

import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.order.infrastructure.mapper.OrderMapper;
import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private final JpaOrderRepository jpaOrderRepository;

    public OrderRepositoryImpl(JpaOrderRepository jpaOrderRepository) {
        this.jpaOrderRepository = jpaOrderRepository;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderMapper.toEntity(order);
        OrderEntity saved = jpaOrderRepository.save(entity);
        return OrderMapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return jpaOrderRepository.findById(orderId).map(OrderMapper::toDomain);
    }

    @Override
    public void deleteById(String orderId) {
        jpaOrderRepository.deleteById(orderId);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return jpaOrderRepository.findByStatus(status.name()).stream()
                .map(OrderMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findScheduledOrdersReadyForFulfillment(LocalDateTime currentTime) {
        return jpaOrderRepository.findScheduledOrdersReadyForFulfillment(currentTime).stream()
                .map(OrderMapper::toDomain)
                .collect(Collectors.toList());
    }
}
