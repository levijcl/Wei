package com.wei.orchestrator.order.query.dto;

import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import java.time.LocalDateTime;

public class OrderSummaryDto {

    private String orderId;
    private OrderStatus status;
    private LocalDateTime scheduledPickupTime;
    private String carrier;
    private String trackingNumber;

    public OrderSummaryDto() {}

    public OrderSummaryDto(
            String orderId,
            String status,
            LocalDateTime scheduledPickupTime,
            String carrier,
            String trackingNumber) {
        this.orderId = orderId;
        this.status = OrderStatus.valueOf(status);
        this.scheduledPickupTime = scheduledPickupTime;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
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

    public LocalDateTime getScheduledPickupTime() {
        return scheduledPickupTime;
    }

    public void setScheduledPickupTime(LocalDateTime scheduledPickupTime) {
        this.scheduledPickupTime = scheduledPickupTime;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}
