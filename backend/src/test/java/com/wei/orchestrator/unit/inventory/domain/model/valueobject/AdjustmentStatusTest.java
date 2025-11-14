package com.wei.orchestrator.unit.inventory.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.model.valueobject.AdjustmentStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AdjustmentStatusTest {

    @Nested
    class IsTerminalMethodTest {
        @Test
        void shouldReturnTrueForCompletedStatus() {
            assertTrue(AdjustmentStatus.COMPLETED.isTerminal());
        }

        @Test
        void shouldReturnTrueForFailedStatus() {
            assertTrue(AdjustmentStatus.FAILED.isTerminal());
        }

        @Test
        void shouldReturnFalseForPendingStatus() {
            assertFalse(AdjustmentStatus.PENDING.isTerminal());
        }

        @Test
        void shouldReturnFalseForProcessingStatus() {
            assertFalse(AdjustmentStatus.PROCESSING.isTerminal());
        }
    }

    @Nested
    class CanProcessMethodTest {
        @Test
        void shouldReturnTrueForPendingStatus() {
            assertTrue(AdjustmentStatus.PENDING.canProcess());
        }

        @Test
        void shouldReturnFalseForProcessingStatus() {
            assertFalse(AdjustmentStatus.PROCESSING.canProcess());
        }

        @Test
        void shouldReturnFalseForCompletedStatus() {
            assertFalse(AdjustmentStatus.COMPLETED.canProcess());
        }

        @Test
        void shouldReturnFalseForFailedStatus() {
            assertFalse(AdjustmentStatus.FAILED.canProcess());
        }
    }

    @Nested
    class CanCompleteMethodTest {
        @Test
        void shouldReturnTrueForProcessingStatus() {
            assertTrue(AdjustmentStatus.PROCESSING.canComplete());
        }

        @Test
        void shouldReturnFalseForPendingStatus() {
            assertFalse(AdjustmentStatus.PENDING.canComplete());
        }

        @Test
        void shouldReturnFalseForCompletedStatus() {
            assertFalse(AdjustmentStatus.COMPLETED.canComplete());
        }

        @Test
        void shouldReturnFalseForFailedStatus() {
            assertFalse(AdjustmentStatus.FAILED.canComplete());
        }
    }

    @Nested
    class CanFailMethodTest {
        @Test
        void shouldReturnTrueForPendingStatus() {
            assertTrue(AdjustmentStatus.PENDING.canFail());
        }

        @Test
        void shouldReturnTrueForProcessingStatus() {
            assertTrue(AdjustmentStatus.PROCESSING.canFail());
        }

        @Test
        void shouldReturnFalseForCompletedStatus() {
            assertFalse(AdjustmentStatus.COMPLETED.canFail());
        }

        @Test
        void shouldReturnFalseForFailedStatus() {
            assertFalse(AdjustmentStatus.FAILED.canFail());
        }
    }

    @Nested
    class EnumValuesTest {
        @Test
        void shouldHaveAllExpectedStatuses() {
            AdjustmentStatus[] statuses = AdjustmentStatus.values();

            assertEquals(4, statuses.length);
            assertEquals(AdjustmentStatus.PENDING, AdjustmentStatus.valueOf("PENDING"));
            assertEquals(AdjustmentStatus.PROCESSING, AdjustmentStatus.valueOf("PROCESSING"));
            assertEquals(AdjustmentStatus.COMPLETED, AdjustmentStatus.valueOf("COMPLETED"));
            assertEquals(AdjustmentStatus.FAILED, AdjustmentStatus.valueOf("FAILED"));
        }
    }
}
