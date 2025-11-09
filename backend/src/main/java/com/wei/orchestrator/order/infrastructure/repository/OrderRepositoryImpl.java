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
        Optional<OrderEntity> existingEntity = jpaOrderRepository.findById(order.getOrderId());
        OrderEntity entityToSave;

        if (existingEntity.isPresent()) {
            entityToSave = existingEntity.get();
            updateExistingEntity(entityToSave, order);
        } else {
            entityToSave = OrderMapper.toEntity(order);
        }

        OrderEntity saved = jpaOrderRepository.save(entityToSave);
        return OrderMapper.toDomain(saved);
    }

    private void updateExistingEntity(OrderEntity entity, Order domain) {
        entity.setStatus(domain.getStatus().name());

        if (domain.getScheduledPickupTime() != null) {
            entity.setScheduledPickupTime(domain.getScheduledPickupTime().getPickupTime());
        } else {
            entity.setScheduledPickupTime(null);
        }

        if (domain.getFulfillmentLeadTime() != null) {
            entity.setFulfillmentLeadTimeMinutes(domain.getFulfillmentLeadTime().getMinutes());
        } else {
            entity.setFulfillmentLeadTimeMinutes(null);
        }

        if (domain.getShipmentInfo() != null) {
            entity.setShipmentCarrier(domain.getShipmentInfo().getCarrier());
            entity.setShipmentTrackingNumber(domain.getShipmentInfo().getTrackingNumber());
        } else {
            entity.setShipmentCarrier(null);
            entity.setShipmentTrackingNumber(null);
        }

        entity.getOrderLineItems().clear();
        domain.getOrderLineItems()
                .forEach(
                        item -> {
                            var lineItemEntity = OrderMapper.toLineItemEntity(item, entity);
                            entity.getOrderLineItems().add(lineItemEntity);
                        });
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
