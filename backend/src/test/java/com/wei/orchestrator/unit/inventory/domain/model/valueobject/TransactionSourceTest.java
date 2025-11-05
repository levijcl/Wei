package com.wei.orchestrator.unit.inventory.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionSource;
import org.junit.jupiter.api.Test;

class TransactionSourceTest {

    @Test
    void shouldIdentifyReservationRelatedSources() {
        assertTrue(TransactionSource.ORDER_RESERVATION.isReservationRelated());
        assertTrue(TransactionSource.RESERVATION_CONSUMED.isReservationRelated());
        assertTrue(TransactionSource.RESERVATION_RELEASED.isReservationRelated());
        assertFalse(TransactionSource.PICKING_TASK_COMPLETED.isReservationRelated());
        assertFalse(TransactionSource.MANUAL_ADJUSTMENT.isReservationRelated());
    }

    @Test
    void shouldIdentifyTaskRelatedSources() {
        assertTrue(TransactionSource.PICKING_TASK_COMPLETED.isTaskRelated());
        assertTrue(TransactionSource.PUTAWAY_TASK_COMPLETED.isTaskRelated());
        assertFalse(TransactionSource.ORDER_RESERVATION.isTaskRelated());
        assertFalse(TransactionSource.MANUAL_ADJUSTMENT.isTaskRelated());
    }

    @Test
    void shouldIdentifyAdjustmentRelatedSources() {
        assertTrue(TransactionSource.MANUAL_ADJUSTMENT.isAdjustmentRelated());
        assertTrue(TransactionSource.CYCLE_COUNT_ADJUSTMENT.isAdjustmentRelated());
        assertFalse(TransactionSource.ORDER_RESERVATION.isAdjustmentRelated());
        assertFalse(TransactionSource.PICKING_TASK_COMPLETED.isAdjustmentRelated());
    }
}
