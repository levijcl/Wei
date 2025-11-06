package com.wei.orchestrator.order.domain.event;

import java.time.LocalDateTime;

public class OrderFulfillmentFailedEvent {
    private final String orderId;
    private final String reason;
    private final LocalDateTime occurredAt;

    public OrderFulfillmentFailedEvent(String orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
        this.occurredAt = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "OrderFulfillmentFailedEvent{"
                + "orderId='"
                + orderId
                + '\''
                + ", reason='"
                + reason
                + '\''
                + ", occurredAt="
                + occurredAt
                + '}';
    }
}
