package com.wei.orchestrator.order.domain.model.valueobject;

public enum OrderStatus {
    CREATED,
    SCHEDULED,
    AWAITING_FULFILLMENT,
    RESERVED,
    COMMITTED,
    SHIPPED,
    FAILED_TO_RESERVE
}
