package com.wei.orchestrator.inventory.domain.event;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionSource;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType;
import java.time.LocalDateTime;

public final class InventoryTransactionFailedEvent {
    private final String transactionId;
    private final TransactionType type;
    private final TransactionSource source;
    private final String reason;
    private final LocalDateTime occurredAt;

    public InventoryTransactionFailedEvent(
            String transactionId,
            TransactionType type,
            TransactionSource source,
            String reason,
            LocalDateTime occurredAt) {
        this.transactionId = transactionId;
        this.type = type;
        this.source = source;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
