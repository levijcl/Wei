package com.wei.orchestrator.order.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "status")
    private String status;

    @Column(name = "scheduled_pickup_time")
    private LocalDateTime scheduledPickupTime;

    @Column(name = "fulfillment_lead_time_minutes")
    private Long fulfillmentLeadTimeMinutes;

    @Column(name = "reservation_warehouse_id")
    private String reservationWarehouseId;

    @Column(name = "reservation_reserved_qty")
    private Integer reservationReservedQty;

    @Column(name = "reservation_status")
    private String reservationStatus;

    @Column(name = "shipment_carrier")
    private String shipmentCarrier;

    @Column(name = "shipment_tracking_number")
    private String shipmentTrackingNumber;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineItemEntity> orderLineItems = new ArrayList<>();

    public OrderEntity() {}

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getScheduledPickupTime() {
        return scheduledPickupTime;
    }

    public void setScheduledPickupTime(LocalDateTime scheduledPickupTime) {
        this.scheduledPickupTime = scheduledPickupTime;
    }

    public Long getFulfillmentLeadTimeMinutes() {
        return fulfillmentLeadTimeMinutes;
    }

    public void setFulfillmentLeadTimeMinutes(Long fulfillmentLeadTimeMinutes) {
        this.fulfillmentLeadTimeMinutes = fulfillmentLeadTimeMinutes;
    }

    public String getReservationWarehouseId() {
        return reservationWarehouseId;
    }

    public void setReservationWarehouseId(String reservationWarehouseId) {
        this.reservationWarehouseId = reservationWarehouseId;
    }

    public Integer getReservationReservedQty() {
        return reservationReservedQty;
    }

    public void setReservationReservedQty(Integer reservationReservedQty) {
        this.reservationReservedQty = reservationReservedQty;
    }

    public String getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(String reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    public String getShipmentCarrier() {
        return shipmentCarrier;
    }

    public void setShipmentCarrier(String shipmentCarrier) {
        this.shipmentCarrier = shipmentCarrier;
    }

    public String getShipmentTrackingNumber() {
        return shipmentTrackingNumber;
    }

    public void setShipmentTrackingNumber(String shipmentTrackingNumber) {
        this.shipmentTrackingNumber = shipmentTrackingNumber;
    }

    public List<OrderLineItemEntity> getOrderLineItems() {
        return orderLineItems;
    }

    public void setOrderLineItems(List<OrderLineItemEntity> orderLineItems) {
        this.orderLineItems = orderLineItems;
    }

    public void addOrderLineItem(OrderLineItemEntity item) {
        orderLineItems.add(item);
        item.setOrder(this);
    }

    public void removeOrderLineItem(OrderLineItemEntity item) {
        orderLineItems.remove(item);
        item.setOrder(null);
    }
}
