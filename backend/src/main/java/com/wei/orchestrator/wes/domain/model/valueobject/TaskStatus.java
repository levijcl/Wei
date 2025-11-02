package com.wei.orchestrator.wes.domain.model.valueobject;

public enum TaskStatus {
    PENDING,
    SUBMITTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELED;
    }

    public boolean canSubmit() {
        return this == PENDING;
    }

    public boolean canUpdateFromWes() {
        return this == SUBMITTED || this == IN_PROGRESS;
    }

    public boolean canCancel() {
        return this == PENDING || this == SUBMITTED || this == IN_PROGRESS;
    }
}
