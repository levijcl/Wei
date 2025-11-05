package com.wei.orchestrator.inventory.domain.event;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionSource;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType;
import java.time.LocalDateTime;

public final class InventoryTransactionCompletedEvent {
    private final String transactionId;
    private final TransactionType type;
    private final TransactionSource source;
    private final String sourceReferenceId;
    private final LocalDateTime occurredAt;

    public InventoryTransactionCompletedEvent(
            String transactionId,
            TransactionType type,
            TransactionSource source,
            String sourceReferenceId,
            LocalDateTime occurredAt) {
        this.transactionId = transactionId;
        this.type = type;
        this.source = source;
        this.sourceReferenceId = sourceReferenceId;
        this.occurredAt = occurredAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionSource getSource() {
        return source;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
