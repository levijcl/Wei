package com.wei.orchestrator.order.query.dto;

import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDetailDto {

    private String orderId;
    private OrderStatus status;
    private LocalDateTime scheduledPickupTime;
    private Long fulfillmentLeadTimeMinutes;
    private String carrier;
    private String trackingNumber;
    private List<OrderLineItemDto> lineItems;

    public OrderDetailDto() {}

    public OrderDetailDto(
            String orderId,
            OrderStatus status,
            LocalDateTime scheduledPickupTime,
            Long fulfillmentLeadTimeMinutes,
            String carrier,
            String trackingNumber,
            List<OrderLineItemDto> lineItems) {
        this.orderId = orderId;
        this.status = status;
        this.scheduledPickupTime = scheduledPickupTime;
        this.fulfillmentLeadTimeMinutes = fulfillmentLeadTimeMinutes;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.lineItems = lineItems;
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

    public Long getFulfillmentLeadTimeMinutes() {
        return fulfillmentLeadTimeMinutes;
    }

    public void setFulfillmentLeadTimeMinutes(Long fulfillmentLeadTimeMinutes) {
        this.fulfillmentLeadTimeMinutes = fulfillmentLeadTimeMinutes;
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

    public List<OrderLineItemDto> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<OrderLineItemDto> lineItems) {
        this.lineItems = lineItems;
    }
}
