package com.wei.orchestrator.unit.inventory.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType;
import org.junit.jupiter.api.Test;

class TransactionTypeTest {

    @Test
    void shouldIdentifyInbound() {
        assertTrue(TransactionType.INBOUND.isInbound());
        assertFalse(TransactionType.OUTBOUND.isInbound());
        assertFalse(TransactionType.ADJUSTMENT.isInbound());
    }

    @Test
    void shouldIdentifyOutbound() {
        assertTrue(TransactionType.OUTBOUND.isOutbound());
        assertFalse(TransactionType.INBOUND.isOutbound());
        assertFalse(TransactionType.ADJUSTMENT.isOutbound());
    }

    @Test
    void shouldIdentifyAdjustment() {
        assertTrue(TransactionType.ADJUSTMENT.isAdjustment());
        assertFalse(TransactionType.INBOUND.isAdjustment());
        assertFalse(TransactionType.OUTBOUND.isAdjustment());
    }
}
