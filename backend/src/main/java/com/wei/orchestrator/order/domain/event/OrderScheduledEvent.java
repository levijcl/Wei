package com.wei.orchestrator.order.domain.event;

import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.time.LocalDateTime;
import java.util.UUID;

public class OrderScheduledEvent implements DomainEvent {
    private final String orderId;
    private final LocalDateTime scheduledPickupTime;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    public OrderScheduledEvent(String orderId, LocalDateTime scheduledPickupTime) {
        this.orderId = orderId;
        this.scheduledPickupTime = scheduledPickupTime;
        this.occurredAt = LocalDateTime.now();
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public OrderScheduledEvent(
            String orderId, LocalDateTime scheduledPickupTime, TriggerContext triggerContext) {
        this.orderId = orderId;
        this.scheduledPickupTime = scheduledPickupTime;
        this.occurredAt = LocalDateTime.now();
        this.triggerContext = triggerContext;
        this.correlationId =
                triggerContext != null ? triggerContext.getCorrelationId() : UUID.randomUUID();
    }

    public String getOrderId() {
        return orderId;
    }

    public LocalDateTime getScheduledPickupTime() {
        return scheduledPickupTime;
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
        return "OrderScheduledEvent{"
                + "orderId='"
                + orderId
                + '\''
                + ", scheduledPickupTime="
                + scheduledPickupTime
                + ", occurredAt="
                + occurredAt
                + ", correlationId="
                + correlationId
                + '}';
    }
}
