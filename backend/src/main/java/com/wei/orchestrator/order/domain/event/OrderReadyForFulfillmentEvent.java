package com.wei.orchestrator.order.domain.event;

import java.time.LocalDateTime;

public class OrderReadyForFulfillmentEvent {
    private final String orderId;
    private final LocalDateTime occurredAt;

    public OrderReadyForFulfillmentEvent(String orderId) {
        this.orderId = orderId;
        this.occurredAt = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "OrderReadyForFulfillmentEvent{"
                + "orderId='"
                + orderId
                + '\''
                + ", occurredAt="
                + occurredAt
                + '}';
    }
}
