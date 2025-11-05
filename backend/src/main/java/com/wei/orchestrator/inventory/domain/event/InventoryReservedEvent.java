package com.wei.orchestrator.inventory.domain.event;

import java.time.LocalDateTime;

public final class InventoryReservedEvent {
    private final String transactionId;
    private final String orderId;
    private final String externalReservationId;
    private final LocalDateTime occurredAt;

    public InventoryReservedEvent(
            String transactionId,
            String orderId,
            String externalReservationId,
            LocalDateTime occurredAt) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.externalReservationId = externalReservationId;
        this.occurredAt = occurredAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getExternalReservationId() {
        return externalReservationId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
