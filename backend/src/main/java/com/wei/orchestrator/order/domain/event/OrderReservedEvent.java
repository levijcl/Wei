package com.wei.orchestrator.order.domain.event;

import java.time.LocalDateTime;
import java.util.List;

public class OrderReservedEvent {
    private final String orderId;
    private final List<String> reservedLineItemIds;
    private final LocalDateTime occurredAt;

    public OrderReservedEvent(String orderId, List<String> reservedLineItemIds) {
        this.orderId = orderId;
        this.reservedLineItemIds = List.copyOf(reservedLineItemIds);
        this.occurredAt = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public List<String> getReservedLineItemIds() {
        return reservedLineItemIds;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
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
