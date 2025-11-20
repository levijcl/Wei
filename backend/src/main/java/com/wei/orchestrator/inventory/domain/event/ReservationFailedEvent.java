package com.wei.orchestrator.inventory.domain.event;

import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ReservationFailedEvent implements DomainEvent {
    private final String transactionId;
    private final String orderId;
    private final String reason;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    public ReservationFailedEvent(
            String transactionId, String orderId, String reason, LocalDateTime occurredAt) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public ReservationFailedEvent(
            String transactionId,
            String orderId,
            String reason,
            LocalDateTime occurredAt,
            TriggerContext triggerContext) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.triggerContext = triggerContext;
        this.correlationId =
                triggerContext != null ? triggerContext.getCorrelationId() : UUID.randomUUID();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getOrderId() {
        return orderId;
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
