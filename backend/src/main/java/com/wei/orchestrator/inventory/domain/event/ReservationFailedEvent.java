package com.wei.orchestrator.inventory.domain.event;

import java.time.LocalDateTime;

public final class ReservationFailedEvent {
    private final String transactionId;
    private final String orderId;
    private final String reason;
    private final LocalDateTime occurredAt;

    public ReservationFailedEvent(
            String transactionId, String orderId, String reason, LocalDateTime occurredAt) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public String getTransactionId() {
        return transactionId;
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
}
