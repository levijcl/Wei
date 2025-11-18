package com.wei.orchestrator.order.domain.event;

import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderReservedEvent implements DomainEvent {
    private final String orderId;
    private final List<String> reservedLineItemIds;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    public OrderReservedEvent(String orderId, List<String> reservedLineItemIds) {
        this.orderId = orderId;
        this.reservedLineItemIds = List.copyOf(reservedLineItemIds);
        this.occurredAt = LocalDateTime.now();
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public OrderReservedEvent(
            String orderId, List<String> reservedLineItemIds, TriggerContext triggerContext) {
        this.orderId = orderId;
        this.reservedLineItemIds = List.copyOf(reservedLineItemIds);
        this.occurredAt = LocalDateTime.now();
        this.triggerContext = triggerContext;
        this.correlationId =
                triggerContext != null ? triggerContext.getCorrelationId() : UUID.randomUUID();
    }

    public String getOrderId() {
        return orderId;
    }

    public List<String> getReservedLineItemIds() {
        return reservedLineItemIds;
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
        return "OrderReservedEvent{"
                + "orderId='"
                + orderId
                + '\''
                + ", reservedLineItemIds="
                + reservedLineItemIds
                + ", occurredAt="
                + occurredAt
                + '}';
    }
}
