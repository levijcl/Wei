package com.wei.orchestrator.order.domain.model.valueobject;

public enum OrderStatus {
    CREATED,
    SCHEDULED,
    AWAITING_FULFILLMENT,
    PARTIALLY_RESERVED,
    RESERVED,
    PARTIALLY_COMMITTED,
    COMMITTED,
    SHIPPED,
    FAILED_TO_RESERVE
}
