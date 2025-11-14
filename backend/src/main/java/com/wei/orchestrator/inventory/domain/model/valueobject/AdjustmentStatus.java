package com.wei.orchestrator.inventory.domain.model.valueobject;

public enum AdjustmentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    public boolean canProcess() {
        return this == PENDING;
    }

    public boolean canComplete() {
        return this == PROCESSING;
    }

    public boolean canFail() {
        return this == PENDING || this == PROCESSING;
    }
}
