package com.wei.orchestrator.inventory.domain.event;

import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ReservationReleasedEvent implements DomainEvent {
    private final String transactionId;
    private final String orderId;
    private final String externalReservationId;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    public ReservationReleasedEvent(
            String transactionId,
            String orderId,
            String externalReservationId,
            LocalDateTime occurredAt) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.externalReservationId = externalReservationId;
        this.occurredAt = occurredAt;
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public ReservationReleasedEvent(
            String transactionId,
            String orderId,
            String externalReservationId,
            LocalDateTime occurredAt,
            TriggerContext triggerContext) {
        this.transactionId = transactionId;
        this.orderId = orderId;
        this.externalReservationId = externalReservationId;
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

    public String getExternalReservationId() {
        return externalReservationId;
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
