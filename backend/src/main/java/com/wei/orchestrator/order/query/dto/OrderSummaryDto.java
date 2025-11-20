package com.wei.orchestrator.order.query.dto;

import java.time.LocalDateTime;

public class OrderSummaryDto {

    private String orderId;
    private String status;
    private LocalDateTime scheduledPickupTime;
    private String carrier;
    private String trackingNumber;
    private String description;

    public OrderSummaryDto(
            String orderId,
            String status,
            LocalDateTime scheduledPickupTime,
            String carrier,
            String trackingNumber,
            Long lineCount,
            Long totalQuantity) {
        this.orderId = orderId;
        this.status = status;
        this.scheduledPickupTime = scheduledPickupTime;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.description = lineCount + " lines / " + totalQuantity + " pcs";
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
