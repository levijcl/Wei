package com.wei.orchestrator.order.domain.event;

import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.time.LocalDateTime;
import java.util.UUID;

public class OrderReadyForFulfillmentEvent implements DomainEvent {
    private final String orderId;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    public OrderReadyForFulfillmentEvent(String orderId) {
        this.orderId = orderId;
        this.occurredAt = LocalDateTime.now();
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public OrderReadyForFulfillmentEvent(String orderId, TriggerContext triggerContext) {
        this.orderId = orderId;
        this.occurredAt = LocalDateTime.now();
        this.triggerContext = triggerContext;
        this.correlationId =
                triggerContext != null ? triggerContext.getCorrelationId() : UUID.randomUUID();
    }

    public String getOrderId() {
        return orderId;
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

    @Override
    public String toString() {
        return "OrderReadyForFulfillmentEvent{"
                + "orderId='"
                + orderId
                + '\''
                + ", occurredAt="
                + occurredAt
                + ", correlationId="
                + correlationId
                + '}';
    }
}
