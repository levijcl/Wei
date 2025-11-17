package com.wei.orchestrator.inventory.domain.event;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionSource;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType;
import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.time.LocalDateTime;
import java.util.UUID;

public final class InventoryTransactionFailedEvent implements DomainEvent {
    private final String transactionId;
    private final TransactionType type;
    private final TransactionSource source;
    private final String reason;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

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
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public InventoryTransactionFailedEvent(
            String transactionId,
            TransactionType type,
            TransactionSource source,
            String reason,
            LocalDateTime occurredAt,
            TriggerContext triggerContext) {
        this.transactionId = transactionId;
        this.type = type;
        this.source = source;
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.triggerContext = triggerContext;
        this.correlationId =
                triggerContext != null ? triggerContext.getCorrelationId() : UUID.randomUUID();
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

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public TriggerContext getTriggerContext() {
        return triggerContext;
    }
}
