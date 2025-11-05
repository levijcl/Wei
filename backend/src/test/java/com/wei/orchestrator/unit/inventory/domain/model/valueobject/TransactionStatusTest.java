package com.wei.orchestrator.unit.inventory.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionStatus;
import org.junit.jupiter.api.Test;

class TransactionStatusTest {

    @Test
    void shouldIdentifyTerminalStatuses() {
        assertTrue(TransactionStatus.COMPLETED.isTerminal());
        assertTrue(TransactionStatus.FAILED.isTerminal());
        assertFalse(TransactionStatus.PENDING.isTerminal());
        assertFalse(TransactionStatus.PROCESSING.isTerminal());
    }

    @Test
    void shouldIdentifyCanProcess() {
        assertTrue(TransactionStatus.PENDING.canProcess());
        assertFalse(TransactionStatus.PROCESSING.canProcess());
        assertFalse(TransactionStatus.COMPLETED.canProcess());
        assertFalse(TransactionStatus.FAILED.canProcess());
    }

    @Test
    void shouldIdentifyCanComplete() {
        assertTrue(TransactionStatus.PROCESSING.canComplete());
        assertFalse(TransactionStatus.PENDING.canComplete());
        assertFalse(TransactionStatus.COMPLETED.canComplete());
        assertFalse(TransactionStatus.FAILED.canComplete());
    }

    @Test
    void shouldIdentifyCanFail() {
        assertTrue(TransactionStatus.PENDING.canFail());
        assertTrue(TransactionStatus.PROCESSING.canFail());
        assertFalse(TransactionStatus.COMPLETED.canFail());
        assertFalse(TransactionStatus.FAILED.canFail());
    }
}
