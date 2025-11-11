package com.wei.orchestrator.order.infrastructure.mapper;

import com.wei.orchestrator.order.domain.model.*;
import com.wei.orchestrator.order.domain.model.valueobject.*;
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

        if (entity.getShipmentCarrier() != null) {
            ShipmentInfo shipmentInfo =
                    new ShipmentInfo(
                            entity.getShipmentCarrier(), entity.getShipmentTrackingNumber());
            order.setShipmentInfo(shipmentInfo);
        }

        return order;
    }

    public static OrderLineItemEntity toLineItemEntity(OrderLineItem domain, OrderEntity parent) {
        OrderLineItemEntity entity = new OrderLineItemEntity();
        entity.setId(domain.getLineItemId());
        entity.setOrder(parent);
        entity.setSku(domain.getSku());
        entity.setQuantity(domain.getQuantity());
        entity.setPrice(domain.getPrice());

        if (domain.getReservationInfo() != null) {
            LineReservationInfo resInfo = domain.getReservationInfo();
            entity.setReservationStatus(resInfo.getStatus().name());
            entity.setReservationTransactionId(resInfo.getTransactionId());
            entity.setReservationExternalReservationId(resInfo.getExternalReservationId());
            entity.setReservationWarehouseId(resInfo.getWarehouseId());
            entity.setReservationFailureReason(resInfo.getFailureReason());
            entity.setReservationReservedAt(resInfo.getReservedAt());
        }

        if (domain.getCommitmentInfo() != null) {
            LineCommitmentInfo comInfo = domain.getCommitmentInfo();
            entity.setCommitmentStatus(comInfo.getStatus().name());
            entity.setCommitmentWesTransactionId(comInfo.getWesTransactionId());
            entity.setCommitmentFailureReason(comInfo.getFailureReason());
            entity.setCommitmentCommittedAt(comInfo.getCommittedAt());
        }

        return entity;
    }

    private static OrderLineItem toLineItemDomain(OrderLineItemEntity entity) {
        OrderLineItem domain =
                new OrderLineItem(entity.getSku(), entity.getQuantity(), entity.getPrice());
        domain.setLineItemId(entity.getId());

        if (entity.getReservationStatus() != null) {
            ReservationStatus resStatus = ReservationStatus.valueOf(entity.getReservationStatus());
            LineReservationInfo resInfo;

            if (resStatus == ReservationStatus.RESERVED) {
                resInfo =
                        LineReservationInfo.reserved(
                                entity.getReservationTransactionId(),
                                entity.getReservationExternalReservationId(),
                                entity.getReservationWarehouseId());
            } else if (resStatus == ReservationStatus.FAILED) {
                resInfo = LineReservationInfo.failed(entity.getReservationFailureReason());
            } else {
                resInfo = LineReservationInfo.pending();
            }

            domain.setReservationInfo(resInfo);
        }

        if (entity.getCommitmentStatus() != null) {
            CommitmentStatus comStatus = CommitmentStatus.valueOf(entity.getCommitmentStatus());
            LineCommitmentInfo comInfo;

            if (comStatus == CommitmentStatus.COMMITTED) {
                comInfo = LineCommitmentInfo.committed(entity.getCommitmentWesTransactionId());
            } else if (comStatus == CommitmentStatus.FAILED) {
                comInfo = LineCommitmentInfo.failed(entity.getCommitmentFailureReason());
            } else if (comStatus == CommitmentStatus.IN_PROGRESS) {
                comInfo = LineCommitmentInfo.inProgress(entity.getCommitmentWesTransactionId());
            } else {
                comInfo = LineCommitmentInfo.pending();
            }

            domain.setCommitmentInfo(comInfo);
        }

        return domain;
    }
}
