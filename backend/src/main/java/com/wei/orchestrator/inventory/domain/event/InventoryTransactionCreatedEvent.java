package com.wei.orchestrator.inventory.domain.event;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionSource;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType;
import java.time.LocalDateTime;

public final class InventoryTransactionCreatedEvent {
    private final String transactionId;
    private final TransactionType type;
    private final String sourceReferenceId;
    private final TransactionSource source;
    private final LocalDateTime occurredAt;

    public InventoryTransactionCreatedEvent(
            String transactionId,
            TransactionType type,
            String sourceReferenceId,
            TransactionSource source,
            LocalDateTime occurredAt) {
        this.transactionId = transactionId;
        this.type = type;
        this.sourceReferenceId = sourceReferenceId;
        this.source = source;
        this.occurredAt = occurredAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }

    public TransactionSource getSource() {
        return source;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
