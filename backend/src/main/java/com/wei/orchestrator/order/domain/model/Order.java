package com.wei.orchestrator.order.domain.model;

import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private String orderId;
    private OrderStatus status;
    private ReservationInfo reservationInfo;
    private ShipmentInfo shipmentInfo;
    private List<OrderLineItem> orderLineItems;

    public Order() {
        this.orderLineItems = new ArrayList<>();
    }

    public Order(String orderId, List<OrderLineItem> orderLineItems) {
        if (orderLineItems == null || orderLineItems.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line item");
        }
        this.orderId = orderId;
        this.status = OrderStatus.CREATED;
        this.orderLineItems = new ArrayList<>(orderLineItems);
    }

    public void createOrder() {
        if (this.orderLineItems == null || this.orderLineItems.isEmpty()) {
            throw new IllegalStateException("Order must have at least one line item");
        }
        this.status = OrderStatus.CREATED;
    }

    public void reserveInventory(ReservationInfo reservationInfo) {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Cannot reserve inventory for order in status: " + this.status);
        }
        this.reservationInfo = reservationInfo;
        this.status = OrderStatus.RESERVED;
    }

    public void commitOrder() {
        if (this.status != OrderStatus.RESERVED) {
            throw new IllegalStateException("Cannot commit order in status: " + this.status);
        }
        this.status = OrderStatus.COMMITTED;
    }

    public void markAsShipped(ShipmentInfo shipmentInfo) {
        if (this.status != OrderStatus.COMMITTED) {
            throw new IllegalStateException(
                    "Cannot mark order as shipped in status: " + this.status);
        }
        this.shipmentInfo = shipmentInfo;
        this.status = OrderStatus.SHIPPED;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public ReservationInfo getReservationInfo() {
        return reservationInfo;
    }

    public void setReservationInfo(ReservationInfo reservationInfo) {
        this.reservationInfo = reservationInfo;
    }

    public ShipmentInfo getShipmentInfo() {
        return shipmentInfo;
    }

    public void setShipmentInfo(ShipmentInfo shipmentInfo) {
        this.shipmentInfo = shipmentInfo;
    }

    public List<OrderLineItem> getOrderLineItems() {
        return new ArrayList<>(orderLineItems);
    }

    public void setOrderLineItems(List<OrderLineItem> orderLineItems) {
        this.orderLineItems =
                orderLineItems != null ? new ArrayList<>(orderLineItems) : new ArrayList<>();
    }

    public void addOrderLineItem(OrderLineItem item) {
        this.orderLineItems.add(item);
    }
}
