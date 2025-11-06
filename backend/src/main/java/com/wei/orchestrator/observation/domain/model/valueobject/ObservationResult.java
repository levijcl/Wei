package com.wei.orchestrator.observation.domain.model.valueobject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ObservationResult {
    private final String orderId;
    private final String customerName;
    private final String customerEmail;
    private final String shippingAddress;
    private final String orderType;
    private final String warehouseId;
    private final String status;
    private final LocalDateTime scheduledPickupTime;
    private final List<ObservedOrderItem> items;
    private final LocalDateTime observedAt;

    public ObservationResult(
            String orderId,
            String customerName,
            String customerEmail,
            String shippingAddress,
            String orderType,
            String warehouseId,
            String status,
            LocalDateTime scheduledPickupTime,
            List<ObservedOrderItem> items,
            LocalDateTime observedAt) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (customerEmail == null || customerEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email cannot be null or empty");
        }
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Shipping address cannot be null or empty");
        }
        if (orderType == null || orderType.trim().isEmpty()) {
            throw new IllegalArgumentException("Order type cannot be null or empty");
        }
        if (warehouseId == null || warehouseId.trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or empty");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        if (observedAt == null) {
            throw new IllegalArgumentException("Observed at cannot be null");
        }
        this.orderId = orderId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.shippingAddress = shippingAddress;
        this.orderType = orderType;
        this.warehouseId = warehouseId;
        this.status = status;
        this.scheduledPickupTime = scheduledPickupTime;
        this.items = new ArrayList<>(items);
        this.observedAt = observedAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getScheduledPickupTime() {
        return scheduledPickupTime;
    }

    public List<ObservedOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObservationResult that = (ObservationResult) o;
        return Objects.equals(orderId, that.orderId)
                && Objects.equals(customerName, that.customerName)
                && Objects.equals(customerEmail, that.customerEmail)
                && Objects.equals(shippingAddress, that.shippingAddress)
                && Objects.equals(orderType, that.orderType)
                && Objects.equals(warehouseId, that.warehouseId)
                && Objects.equals(status, that.status)
                && Objects.equals(scheduledPickupTime, that.scheduledPickupTime)
                && Objects.equals(items, that.items)
                && Objects.equals(observedAt, that.observedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                orderId,
                customerName,
                customerEmail,
                shippingAddress,
                orderType,
                warehouseId,
                status,
                scheduledPickupTime,
                items,
                observedAt);
    }

    @Override
    public String toString() {
        return "ObservationResult{"
                + "orderId='"
                + orderId
                + '\''
                + ", customerName='"
                + customerName
                + '\''
                + ", customerEmail='"
                + customerEmail
                + '\''
                + ", shippingAddress='"
                + shippingAddress
                + '\''
                + ", orderType='"
                + orderType
                + '\''
                + ", warehouseId='"
                + warehouseId
                + '\''
                + ", status='"
                + status
                + '\''
                + ", scheduledPickupTime="
                + scheduledPickupTime
                + ", items="
                + items
                + ", observedAt="
                + observedAt
                + '}';
    }
}
