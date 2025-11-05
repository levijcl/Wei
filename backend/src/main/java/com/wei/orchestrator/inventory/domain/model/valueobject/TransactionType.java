package com.wei.orchestrator.inventory.domain.model.valueobject;

public enum TransactionType {
    INBOUND,
    OUTBOUND,
    ADJUSTMENT;

    public boolean isInbound() {
        return this == INBOUND;
    }

    public boolean isOutbound() {
        return this == OUTBOUND;
    }

    public boolean isAdjustment() {
        return this == ADJUSTMENT;
    }
}
