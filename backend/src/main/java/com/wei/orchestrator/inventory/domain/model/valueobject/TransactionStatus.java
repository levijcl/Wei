package com.wei.orchestrator.inventory.domain.model.valueobject;

public enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    public boolean canProcess() {
        return this == PENDING || this == COMPLETED;
    }

    public boolean canComplete() {
        return this == PROCESSING;
    }

    public boolean canFail() {
        return this == PENDING || this == PROCESSING;
    }
}
