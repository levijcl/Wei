package com.wei.orchestrator.order.infrastructure.repository;

import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.order.infrastructure.mapper.OrderMapper;
import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import com.wei.orchestrator.order.infrastructure.persistence.OrderLineItemEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

        // Update line items in-place to preserve audit fields (createdAt)
        Map<String, OrderLineItemEntity> existingItemsMap =
                entity.getOrderLineItems().stream()
                        .collect(Collectors.toMap(OrderLineItemEntity::getId, item -> item));

        Map<String, OrderLineItem> newItemsMap =
                domain.getOrderLineItems().stream()
                        .collect(
                                Collectors.toMap(
                                        com.wei.orchestrator.order.domain.model.OrderLineItem
                                                ::getLineItemId,
                                        item -> item));

        // Remove line items that no longer exist
        entity.getOrderLineItems()
                .removeIf(existingItem -> !newItemsMap.containsKey(existingItem.getId()));

        // Update existing items or add new ones
        domain.getOrderLineItems()
                .forEach(
                        domainItem -> {
                            OrderLineItemEntity existingEntity =
                                    existingItemsMap.get(domainItem.getLineItemId());
                            if (existingEntity != null) {
                                // Update existing entity in-place (preserves createdAt)
                                OrderMapper.updateLineItemEntity(existingEntity, domainItem);
                            } else {
                                // Add new entity (will trigger @PrePersist)
                                var newEntity = OrderMapper.toLineItemEntity(domainItem, entity);
                                entity.getOrderLineItems().add(newEntity);
                            }
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
