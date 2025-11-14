package com.wei.orchestrator.unit.inventory.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.event.InventoryAdjustmentAppliedEvent;
import com.wei.orchestrator.inventory.domain.event.InventoryDiscrepancyDetectedEvent;
import com.wei.orchestrator.inventory.domain.model.InventoryAdjustment;
import com.wei.orchestrator.inventory.domain.model.valueobject.AdjustmentStatus;
import com.wei.orchestrator.inventory.domain.model.valueobject.DiscrepancyLog;
import com.wei.orchestrator.observation.domain.model.valueobject.StockSnapshot;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InventoryAdjustmentTest {

    @Nested
    class DetectDiscrepancyMethodTest {
        @Test
        void shouldDetectDiscrepancyWhenWesHasMoreStock() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            assertNotNull(adjustment);
            assertNotNull(adjustment.getAdjustmentId());
            assertEquals(AdjustmentStatus.PENDING, adjustment.getStatus());
            assertNotNull(adjustment.getCreatedAt());
            assertTrue(adjustment.hasDiscrepancies());
            assertEquals(1, adjustment.getDiscrepancyLogs().size());

            DiscrepancyLog log = adjustment.getDiscrepancyLogs().get(0);
            assertEquals("SKU-001", log.getSku());
            assertEquals("WH-01", log.getWarehouseId());
            assertEquals(15, log.getExpectedQuantity());
            assertEquals(10, log.getActualQuantity());
            assertEquals(-5, log.getDifference());
            assertTrue(log.isUnderstock());
        }

        @Test
        void shouldDetectDiscrepancyWhenWesHasLessStock() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-002", 20, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-002", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            assertTrue(adjustment.hasDiscrepancies());
            assertEquals(1, adjustment.getDiscrepancyLogs().size());

            DiscrepancyLog log = adjustment.getDiscrepancyLogs().get(0);
            assertEquals(15, log.getExpectedQuantity());
            assertEquals(20, log.getActualQuantity());
            assertEquals(5, log.getDifference());
            assertTrue(log.isOverstock());
        }

        @Test
        void shouldDetectDiscrepancyWhenSkuOnlyInWes() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-003", 25, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            assertTrue(adjustment.hasDiscrepancies());
            assertEquals(1, adjustment.getDiscrepancyLogs().size());

            DiscrepancyLog log = adjustment.getDiscrepancyLogs().get(0);
            assertEquals("SKU-003", log.getSku());
            assertEquals(25, log.getExpectedQuantity());
            assertEquals(0, log.getActualQuantity());
            assertEquals(-25, log.getDifference());
        }

        @Test
        void shouldDetectDiscrepancyWhenSkuOnlyInInventory() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-004", 30, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            assertTrue(adjustment.hasDiscrepancies());
            assertEquals(1, adjustment.getDiscrepancyLogs().size());

            DiscrepancyLog log = adjustment.getDiscrepancyLogs().get(0);
            assertEquals("SKU-004", log.getSku());
            assertEquals(0, log.getExpectedQuantity());
            assertEquals(30, log.getActualQuantity());
            assertEquals(30, log.getDifference());
        }

        @Test
        void shouldNotDetectDiscrepancyWhenQuantitiesMatch() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-005", 50, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-005", 50, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            assertFalse(adjustment.hasDiscrepancies());
            assertEquals(0, adjustment.getDiscrepancyLogs().size());
            assertEquals(0, adjustment.getDomainEvents().size());
        }

        @Test
        void shouldDetectMultipleDiscrepancies() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));
            inventorySnapshots.add(new StockSnapshot("SKU-002", 20, "WH-01", LocalDateTime.now()));
            inventorySnapshots.add(new StockSnapshot("SKU-003", 30, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));
            wesSnapshots.add(new StockSnapshot("SKU-002", 20, "WH-01", LocalDateTime.now()));
            wesSnapshots.add(new StockSnapshot("SKU-004", 40, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            assertTrue(adjustment.hasDiscrepancies());
            assertEquals(3, adjustment.getDiscrepancyLogs().size());
        }

        @Test
        void shouldHandleMultipleWarehouses() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));
            inventorySnapshots.add(new StockSnapshot("SKU-001", 20, "WH-02", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));
            wesSnapshots.add(new StockSnapshot("SKU-001", 25, "WH-02", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            assertTrue(adjustment.hasDiscrepancies());
            assertEquals(2, adjustment.getDiscrepancyLogs().size());

            boolean foundWh01 =
                    adjustment.getDiscrepancyLogs().stream()
                            .anyMatch(
                                    log ->
                                            log.getWarehouseId().equals("WH-01")
                                                    && log.getDifference() == -5);
            boolean foundWh02 =
                    adjustment.getDiscrepancyLogs().stream()
                            .anyMatch(
                                    log ->
                                            log.getWarehouseId().equals("WH-02")
                                                    && log.getDifference() == -5);

            assertTrue(foundWh01);
            assertTrue(foundWh02);
        }

        @Test
        void shouldEmitDiscrepancyDetectedEventWhenDiscrepanciesFound() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            List<Object> events = adjustment.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(InventoryDiscrepancyDetectedEvent.class, events.get(0));

            InventoryDiscrepancyDetectedEvent event =
                    (InventoryDiscrepancyDetectedEvent) events.get(0);
            assertEquals(adjustment.getAdjustmentId(), event.getAdjustmentId());
            assertEquals(1, event.getDiscrepancies().size());
        }

        @Test
        void shouldNotEmitEventWhenNoDiscrepanciesFound() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            assertEquals(0, adjustment.getDomainEvents().size());
        }

        @Test
        void shouldThrowExceptionWhenInventorySnapshotsIsNull() {
            List<StockSnapshot> wesSnapshots = new ArrayList<>();

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> InventoryAdjustment.detectDiscrepancy(null, wesSnapshots));
            assertTrue(exception.getMessage().contains("Snapshots cannot be null"));
        }

        @Test
        void shouldThrowExceptionWhenWesSnapshotsIsNull() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> InventoryAdjustment.detectDiscrepancy(inventorySnapshots, null));
            assertTrue(exception.getMessage().contains("Snapshots cannot be null"));
        }
    }

    @Nested
    class MarkAsProcessingMethodTest {
        @Test
        void shouldMarkAdjustmentAsProcessing() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            adjustment.markAsProcessing();

            assertEquals(AdjustmentStatus.PROCESSING, adjustment.getStatus());
        }

        @Test
        void shouldThrowExceptionWhenMarkingNonPendingAdjustmentAsProcessing() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);
            adjustment.markAsProcessing();

            IllegalStateException exception =
                    assertThrows(IllegalStateException.class, () -> adjustment.markAsProcessing());
            assertTrue(exception.getMessage().contains("Cannot process adjustment in status"));
        }
    }

    @Nested
    class ApplyAdjustmentMethodTest {
        @Test
        void shouldApplyAdjustmentWithTransactionId() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);
            adjustment.clearDomainEvents();

            adjustment.applyAdjustment("TRANS-001");

            assertEquals(AdjustmentStatus.PROCESSING, adjustment.getStatus());
            assertEquals("TRANS-001", adjustment.getAppliedTransactionId());

            List<Object> events = adjustment.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(InventoryAdjustmentAppliedEvent.class, events.get(0));

            InventoryAdjustmentAppliedEvent event = (InventoryAdjustmentAppliedEvent) events.get(0);
            assertEquals(adjustment.getAdjustmentId(), event.getAdjustmentId());
            assertEquals("TRANS-001", event.getTransactionId());
        }

        @Test
        void shouldThrowExceptionWhenApplyingWithNullTransactionId() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> adjustment.applyAdjustment(null));
            assertTrue(exception.getMessage().contains("Transaction ID cannot be null or blank"));
        }

        @Test
        void shouldThrowExceptionWhenApplyingWithBlankTransactionId() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> adjustment.applyAdjustment("  "));
            assertTrue(exception.getMessage().contains("Transaction ID cannot be null or blank"));
        }
    }

    @Nested
    class CompleteMethodTest {
        @Test
        void shouldCompleteAdjustment() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);
            adjustment.markAsProcessing();

            adjustment.complete();

            assertEquals(AdjustmentStatus.COMPLETED, adjustment.getStatus());
            assertNotNull(adjustment.getProcessedAt());
        }

        @Test
        void shouldThrowExceptionWhenCompletingAdjustmentInWrongStatus() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            IllegalStateException exception =
                    assertThrows(IllegalStateException.class, () -> adjustment.complete());
            assertTrue(exception.getMessage().contains("Cannot complete adjustment in status"));
        }
    }

    @Nested
    class FailMethodTest {
        @Test
        void shouldFailAdjustment() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            adjustment.fail("System error occurred");

            assertEquals(AdjustmentStatus.FAILED, adjustment.getStatus());
            assertEquals("System error occurred", adjustment.getFailureReason());
            assertNotNull(adjustment.getProcessedAt());
        }

        @Test
        void shouldThrowExceptionWhenFailingAdjustmentInTerminalStatus() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);
            adjustment.markAsProcessing();
            adjustment.complete();

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> adjustment.fail("Cannot fail completed adjustment"));
            assertTrue(exception.getMessage().contains("Cannot fail adjustment in status"));
        }
    }

    @Nested
    class ClearDomainEventsMethodTest {
        @Test
        void shouldClearDomainEvents() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            assertEquals(1, adjustment.getDomainEvents().size());

            adjustment.clearDomainEvents();

            assertEquals(0, adjustment.getDomainEvents().size());
        }
    }

    @Nested
    class GetDiscrepancyLogsMethodTest {
        @Test
        void shouldReturnDefensiveCopyOfDiscrepancyLogs() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            List<StockSnapshot> wesSnapshots = new ArrayList<>();
            wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

            InventoryAdjustment adjustment =
                    InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);

            List<DiscrepancyLog> logs = adjustment.getDiscrepancyLogs();
            logs.add(DiscrepancyLog.of("SKU-002", "WH-01", 5, 3));

            assertEquals(1, adjustment.getDiscrepancyLogs().size());
        }
    }
}
