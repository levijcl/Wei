package com.wei.orchestrator.inventory.domain.event;

import java.time.LocalDateTime;

public final class InventoryAdjustmentAppliedEvent {
    private final String adjustmentId;
    private final String transactionId;
    private final LocalDateTime occurredAt;

    public InventoryAdjustmentAppliedEvent(
            String adjustmentId, String transactionId, LocalDateTime occurredAt) {
        this.adjustmentId = adjustmentId;
        this.transactionId = transactionId;
        this.occurredAt = occurredAt;
    }

    public String getAdjustmentId() {
        return adjustmentId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "InventoryAdjustmentAppliedEvent{"
                + "adjustmentId='"
                + adjustmentId
                + '\''
                + ", transactionId='"
                + transactionId
                + '\''
                + ", occurredAt="
                + occurredAt
                + '}';
    }
}
