package com.wei.orchestrator.order.domain.event;

import java.time.LocalDateTime;

public class OrderScheduledEvent {
    private final String orderId;
    private final LocalDateTime scheduledPickupTime;
    private final LocalDateTime occurredAt;

    public OrderScheduledEvent(String orderId, LocalDateTime scheduledPickupTime) {
        this.orderId = orderId;
        this.scheduledPickupTime = scheduledPickupTime;
        this.occurredAt = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public LocalDateTime getScheduledPickupTime() {
        return scheduledPickupTime;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "OrderScheduledEvent{"
                + "orderId='"
                + orderId
                + '\''
                + ", scheduledPickupTime="
                + scheduledPickupTime
                + ", occurredAt="
                + occurredAt
                + '}';
    }
}
