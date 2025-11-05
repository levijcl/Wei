package com.wei.orchestrator.inventory.domain.event;

import java.time.LocalDateTime;

public final class InventoryReservationRequestedEvent {
    private final String transactionId;
    private final String orderId;
    private final String sku;
    private final String warehouseId;
    private final int quantity;
    private final LocalDateTime occurredAt;

    public InventoryReservationRequestedEvent(
            String transactionId,
            String orderId,
            String sku,
            String warehouseId,
            int quantity,
            LocalDateTime occurredAt) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.occurredAt = occurredAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSku() {
        return sku;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
