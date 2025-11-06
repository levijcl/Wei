package com.wei.orchestrator.order.infrastructure.mapper;

import com.wei.orchestrator.order.domain.model.*;
import com.wei.orchestrator.order.domain.model.valueobject.FulfillmentLeadTime;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.model.valueobject.ScheduledPickupTime;
import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import com.wei.orchestrator.order.infrastructure.persistence.OrderLineItemEntity;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class OrderMapper {

    public static OrderEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }

        OrderEntity entity = new OrderEntity();
        entity.setOrderId(domain.getOrderId());
        entity.setStatus(domain.getStatus().name());

        if (domain.getScheduledPickupTime() != null) {
            entity.setScheduledPickupTime(domain.getScheduledPickupTime().getPickupTime());
        }

        if (domain.getFulfillmentLeadTime() != null) {
            entity.setFulfillmentLeadTimeMinutes(domain.getFulfillmentLeadTime().getMinutes());
        }

        if (domain.getReservationInfo() != null) {
            entity.setReservationWarehouseId(domain.getReservationInfo().getWarehouseId());
            entity.setReservationReservedQty(domain.getReservationInfo().getReservedQty());
            entity.setReservationStatus(domain.getReservationInfo().getStatus());
        }

        if (domain.getShipmentInfo() != null) {
            entity.setShipmentCarrier(domain.getShipmentInfo().getCarrier());
            entity.setShipmentTrackingNumber(domain.getShipmentInfo().getTrackingNumber());
        }

        List<OrderLineItemEntity> itemEntities =
                domain.getOrderLineItems().stream()
                        .map(item -> toLineItemEntity(item, entity))
                        .collect(Collectors.toList());
        entity.setOrderLineItems(itemEntities);

        return entity;
    }

    public static Order toDomain(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        List<OrderLineItem> items =
                entity.getOrderLineItems().stream()
                        .map(OrderMapper::toLineItemDomain)
                        .collect(Collectors.toList());

        Order order = new Order(entity.getOrderId(), items);
        order.setStatus(OrderStatus.valueOf(entity.getStatus()));

        if (entity.getScheduledPickupTime() != null) {
            order.setScheduledPickupTime(new ScheduledPickupTime(entity.getScheduledPickupTime()));
        }

        if (entity.getFulfillmentLeadTimeMinutes() != null) {
            order.setFulfillmentLeadTime(
                    new FulfillmentLeadTime(
                            Duration.ofMinutes(entity.getFulfillmentLeadTimeMinutes())));
        }

        if (entity.getReservationWarehouseId() != null) {
            ReservationInfo reservationInfo =
                    new ReservationInfo(
                            entity.getReservationWarehouseId(),
                            entity.getReservationReservedQty(),
                            entity.getReservationStatus());
            order.setReservationInfo(reservationInfo);
        }

        if (entity.getShipmentCarrier() != null) {
            ShipmentInfo shipmentInfo =
                    new ShipmentInfo(
                            entity.getShipmentCarrier(), entity.getShipmentTrackingNumber());
            order.setShipmentInfo(shipmentInfo);
        }

        return order;
    }

    private static OrderLineItemEntity toLineItemEntity(OrderLineItem domain, OrderEntity parent) {
        OrderLineItemEntity entity = new OrderLineItemEntity();
        entity.setOrder(parent);
        entity.setSku(domain.getSku());
        entity.setQuantity(domain.getQuantity());
        entity.setPrice(domain.getPrice());
        return entity;
    }

    private static OrderLineItem toLineItemDomain(OrderLineItemEntity entity) {
        return new OrderLineItem(entity.getSku(), entity.getQuantity(), entity.getPrice());
    }
}
