package com.wei.orchestrator.inventory.domain.model.valueobject;

public enum TransactionSource {
    ORDER_RESERVATION,
    RESERVATION_CONSUMED,
    RESERVATION_RELEASED,
    PICKING_TASK_COMPLETED,
    PUTAWAY_TASK_COMPLETED,
    MANUAL_ADJUSTMENT,
    CYCLE_COUNT_ADJUSTMENT,
    ORDER_CANCELLATION;

    public boolean isReservationRelated() {
        return this == ORDER_RESERVATION
                || this == RESERVATION_CONSUMED
                || this == RESERVATION_RELEASED;
    }

    public boolean isTaskRelated() {
        return this == PICKING_TASK_COMPLETED || this == PUTAWAY_TASK_COMPLETED;
    }

    public boolean isAdjustmentRelated() {
        return this == MANUAL_ADJUSTMENT || this == CYCLE_COUNT_ADJUSTMENT;
    }
}
