package com.wei.orchestrator.unit.inventory.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.event.*;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InventoryTransactionTest {

    @Nested
    class CreateReservationMethodTest {
        @Test
        void shouldCreateReservationTransaction() {
            InventoryTransaction transaction =
                    InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);

            assertNotNull(transaction);
            assertNotNull(transaction.getTransactionId());
            assertEquals(TransactionType.OUTBOUND, transaction.getType());
            assertEquals(TransactionStatus.PENDING, transaction.getStatus());
            assertEquals(TransactionSource.ORDER_RESERVATION, transaction.getSource());
            assertEquals("ORDER-001", transaction.getSourceReferenceId());
            assertEquals("WH-01", transaction.getWarehouseLocation().getWarehouseId());
            assertEquals(1, transaction.getTransactionLines().size());
            assertEquals("SKU-001", transaction.getTransactionLines().get(0).getSku());
            assertEquals(10, transaction.getTransactionLines().get(0).getQuantity());
            assertNotNull(transaction.getCreatedAt());
        }

        @Test
        void shouldEmitReservationRequestedEventWhenCreatingReservation() {
            InventoryTransaction transaction =
                    InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);

            List<Object> events = transaction.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(InventoryReservationRequestedEvent.class, events.get(0));

            InventoryReservationRequestedEvent event =
                    (InventoryReservationRequestedEvent) events.get(0);
            assertEquals(transaction.getTransactionId(), event.getTransactionId());
            assertEquals("ORDER-001", event.getOrderId());
            assertEquals("SKU-001", event.getSku());
            assertEquals("WH-01", event.getWarehouseId());
            assertEquals(10, event.getQuantity());
        }

        @Test
        void shouldThrowExceptionWhenCreatingReservationWithNullOrderId() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () ->
                                    InventoryTransaction.createReservation(
                                            null, "SKU-001", "WH-01", 10));
            assertTrue(exception.getMessage().contains("Order ID cannot be null or blank"));
        }

        @Test
        void shouldThrowExceptionWhenCreatingReservationWithZeroQuantity() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () ->
                                    InventoryTransaction.createReservation(
                                            "ORDER-001", "SKU-001", "WH-01", 0));
            assertTrue(exception.getMessage().contains("Quantity must be positive"));
        }

        @Test
        void shouldThrowExceptionWhenCreatingReservationWithNegativeQuantity() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () ->
                                    InventoryTransaction.createReservation(
                                            "ORDER-001", "SKU-001", "WH-01", -5));
            assertTrue(exception.getMessage().contains("Quantity must be positive"));
        }

        @Test
        void shouldMarkReservationAsReservedWithExternalId() {
            InventoryTransaction transaction =
                    InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);

            transaction.clearDomainEvents();

            ExternalReservationId externalId = ExternalReservationId.of("EXT-RES-001");
            transaction.markAsReserved(externalId);

            assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
            assertEquals(externalId, transaction.getExternalReservationId());
            assertNotNull(transaction.getCompletedAt());

            List<Object> events = transaction.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(InventoryReservedEvent.class, events.get(0));

            InventoryReservedEvent event = (InventoryReservedEvent) events.get(0);
            assertEquals("EXT-RES-001", event.getExternalReservationId());
        }

        @Test
        void shouldThrowExceptionWhenMarkingNonPendingTransactionAsReserved() {
            InventoryTransaction transaction =
                    InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);
            transaction.markAsReserved(ExternalReservationId.of("EXT-RES-001"));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () ->
                                    transaction.markAsReserved(
                                            ExternalReservationId.of("EXT-RES-002")));
            assertTrue(
                    exception
                            .getMessage()
                            .contains(
                                    "Can only mark PENDING or PROCESSING transaction as reserved"));
        }
    }

    @Nested
    class createOutboundTransactionMethodTest {
        @Test
        void shouldCreateOutboundTransaction() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 5));
            lines.add(TransactionLine.of("SKU-002", 10));

            InventoryTransaction transaction =
                    InventoryTransaction.createOutboundTransaction(
                            "PICKING-TASK-001",
                            TransactionSource.PICKING_TASK_COMPLETED,
                            "WH-01",
                            lines,
                            ExternalReservationId.of("EXT-RES-001"));

            assertNotNull(transaction);
            assertEquals(TransactionType.OUTBOUND, transaction.getType());
            assertEquals(TransactionStatus.PENDING, transaction.getStatus());
            assertEquals(TransactionSource.PICKING_TASK_COMPLETED, transaction.getSource());
            assertEquals("PICKING-TASK-001", transaction.getSourceReferenceId());
            assertEquals(2, transaction.getTransactionLines().size());
            assertEquals("EXT-RES-001", transaction.getExternalReservationId().getValue());
        }

        @Test
        void shouldEmitTransactionCreatedEventWhenCreatingOutboundTransaction() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 5));

            InventoryTransaction transaction =
                    InventoryTransaction.createOutboundTransaction(
                            "PICKING-TASK-001",
                            TransactionSource.PICKING_TASK_COMPLETED,
                            "WH-01",
                            lines,
                            ExternalReservationId.of("EXT-RES-001"));

            List<Object> events = transaction.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(InventoryTransactionCreatedEvent.class, events.get(0));
        }

        @Test
        void shouldThrowExceptionWhenCreatingOutboundTransactionWithEmptyLines() {
            List<TransactionLine> lines = new ArrayList<>();

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () ->
                                    InventoryTransaction.createOutboundTransaction(
                                            "PICKING-TASK-001",
                                            TransactionSource.PICKING_TASK_COMPLETED,
                                            "WH-01",
                                            lines,
                                            null));
            assertTrue(exception.getMessage().contains("Transaction must have at least one line"));
        }

        @Test
        void shouldThrowExceptionWhenCreatingOutboundTransactionWithNegativeQuantity() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", -5));

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () ->
                                    InventoryTransaction.createOutboundTransaction(
                                            "PICKING-TASK-001",
                                            TransactionSource.PICKING_TASK_COMPLETED,
                                            "WH-01",
                                            lines,
                                            null));
            assertTrue(exception.getMessage().contains("Quantity must be positive for OUTBOUND"));
        }
    }

    @Nested
    class createInboundTransactionMethodTest {
        @Test
        void shouldCreateInboundTransaction() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 20));

            InventoryTransaction transaction =
                    InventoryTransaction.createInboundTransaction(
                            "PUTAWAY-TASK-001",
                            TransactionSource.PUTAWAY_TASK_COMPLETED,
                            "WH-01",
                            lines);

            assertNotNull(transaction);
            assertEquals(TransactionType.INBOUND, transaction.getType());
            assertEquals(TransactionStatus.PENDING, transaction.getStatus());
            assertEquals(TransactionSource.PUTAWAY_TASK_COMPLETED, transaction.getSource());
            assertEquals("PUTAWAY-TASK-001", transaction.getSourceReferenceId());
            assertEquals(1, transaction.getTransactionLines().size());
        }
    }

    @Nested
    class createAdjustmentTransactionMethodTest {
        @Test
        void shouldCreateAdjustmentTransactionWithPositiveAdjustment() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 5));

            InventoryTransaction transaction =
                    InventoryTransaction.createAdjustmentTransaction(
                            "ADJ-001", TransactionSource.MANUAL_ADJUSTMENT, "WH-01", lines);

            assertNotNull(transaction);
            assertEquals(TransactionType.ADJUSTMENT, transaction.getType());
            assertEquals(TransactionStatus.PENDING, transaction.getStatus());
            assertEquals(TransactionSource.MANUAL_ADJUSTMENT, transaction.getSource());
        }

        @Test
        void shouldCreateAdjustmentTransactionWithNegativeAdjustment() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", -5));

            InventoryTransaction transaction =
                    InventoryTransaction.createAdjustmentTransaction(
                            "ADJ-001", TransactionSource.CYCLE_COUNT_ADJUSTMENT, "WH-01", lines);

            assertNotNull(transaction);
            assertEquals(TransactionType.ADJUSTMENT, transaction.getType());
            assertEquals(-5, transaction.getTransactionLines().get(0).getQuantity());
        }
    }

    @Nested
    class markAsProcessingMethodTest {
        @Test
        void shouldMarkTransactionAsProcessing() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 10));

            InventoryTransaction transaction =
                    InventoryTransaction.createInboundTransaction(
                            "PUTAWAY-001",
                            TransactionSource.PUTAWAY_TASK_COMPLETED,
                            "WH-01",
                            lines);

            transaction.markAsProcessing();

            assertEquals(TransactionStatus.PROCESSING, transaction.getStatus());
        }
    }

    @Nested
    class completeMethodTest {
        @Test
        void shouldCompleteInboundTransactionAndEmitEvents() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 10));

            InventoryTransaction transaction =
                    InventoryTransaction.createInboundTransaction(
                            "PUTAWAY-001",
                            TransactionSource.PUTAWAY_TASK_COMPLETED,
                            "WH-01",
                            lines);

            transaction.markAsProcessing();
            transaction.clearDomainEvents();
            transaction.complete();

            assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
            assertNotNull(transaction.getCompletedAt());

            List<Object> events = transaction.getDomainEvents();
            assertEquals(2, events.size());
            assertInstanceOf(InventoryIncreasedEvent.class, events.get(0));
            assertInstanceOf(InventoryTransactionCompletedEvent.class, events.get(1));
        }

        @Test
        void shouldCompleteOutboundTransactionAndEmitEvents() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 10));

            InventoryTransaction transaction =
                    InventoryTransaction.createOutboundTransaction(
                            "PICKING-001",
                            TransactionSource.PICKING_TASK_COMPLETED,
                            "WH-01",
                            lines,
                            null);

            transaction.markAsProcessing();
            transaction.clearDomainEvents();
            transaction.complete();

            assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());

            List<Object> events = transaction.getDomainEvents();
            assertEquals(2, events.size());
            assertInstanceOf(InventoryDecreasedEvent.class, events.get(0));
            assertInstanceOf(InventoryTransactionCompletedEvent.class, events.get(1));
        }

        @Test
        void shouldCompleteOutboundTransactionWithReservationAndEmitReservationConsumedEvent() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 10));

            InventoryTransaction transaction =
                    InventoryTransaction.createOutboundTransaction(
                            "ORDER-001",
                            TransactionSource.RESERVATION_CONSUMED,
                            "WH-01",
                            lines,
                            ExternalReservationId.of("EXT-RES-001"));

            transaction.markAsProcessing();
            transaction.clearDomainEvents();
            transaction.complete();

            List<Object> events = transaction.getDomainEvents();
            assertEquals(3, events.size());
            assertInstanceOf(ReservationConsumedEvent.class, events.get(0));
            assertInstanceOf(InventoryDecreasedEvent.class, events.get(1));
            assertInstanceOf(InventoryTransactionCompletedEvent.class, events.get(2));
        }

        @Test
        void shouldCompleteAdjustmentTransactionAndEmitEvents() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", -3));

            InventoryTransaction transaction =
                    InventoryTransaction.createAdjustmentTransaction(
                            "ADJ-001", TransactionSource.MANUAL_ADJUSTMENT, "WH-01", lines);

            transaction.markAsProcessing();
            transaction.clearDomainEvents();
            transaction.complete();

            List<Object> events = transaction.getDomainEvents();
            assertEquals(2, events.size());
            assertInstanceOf(InventoryAdjustedEvent.class, events.get(0));
            assertInstanceOf(InventoryTransactionCompletedEvent.class, events.get(1));
        }

        @Test
        void shouldThrowExceptionWhenCompletingTransactionInWrongStatus() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 10));

            InventoryTransaction transaction =
                    InventoryTransaction.createInboundTransaction(
                            "PUTAWAY-001",
                            TransactionSource.PUTAWAY_TASK_COMPLETED,
                            "WH-01",
                            lines);

            IllegalStateException exception =
                    assertThrows(IllegalStateException.class, () -> transaction.complete());
            assertTrue(exception.getMessage().contains("Cannot complete transaction in status"));
        }
    }

    @Nested
    class failMethodTest {
        @Test
        void shouldFailTransactionAndEmitEvents() {
            InventoryTransaction transaction =
                    InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);

            transaction.clearDomainEvents();
            transaction.fail("Insufficient inventory");

            assertEquals(TransactionStatus.FAILED, transaction.getStatus());
            assertEquals("Insufficient inventory", transaction.getFailureReason());
            assertNotNull(transaction.getCompletedAt());

            List<Object> events = transaction.getDomainEvents();
            assertEquals(2, events.size());
            assertInstanceOf(ReservationFailedEvent.class, events.get(0));
            assertInstanceOf(InventoryTransactionFailedEvent.class, events.get(1));
        }

        @Test
        void shouldFailNonReservationTransactionWithoutReservationFailedEvent() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 10));

            InventoryTransaction transaction =
                    InventoryTransaction.createInboundTransaction(
                            "PUTAWAY-001",
                            TransactionSource.PUTAWAY_TASK_COMPLETED,
                            "WH-01",
                            lines);

            transaction.clearDomainEvents();
            transaction.fail("System error");

            List<Object> events = transaction.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(InventoryTransactionFailedEvent.class, events.get(0));
        }
    }

    @Nested
    class releaseReservationMethodTest {
        @Test
        void shouldReleaseReservation() {
            InventoryTransaction transaction =
                    InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);
            transaction.markAsReserved(ExternalReservationId.of("EXT-RES-001"));

            transaction.setStatus(TransactionStatus.PENDING);
            transaction.clearDomainEvents();

            transaction.releaseReservation();

            assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
            assertNotNull(transaction.getCompletedAt());

            List<Object> events = transaction.getDomainEvents();
            assertEquals(2, events.size());
            assertInstanceOf(ReservationReleasedEvent.class, events.get(0));
            assertInstanceOf(InventoryTransactionCompletedEvent.class, events.get(1));

            ReservationReleasedEvent event = (ReservationReleasedEvent) events.get(0);
            assertEquals("EXT-RES-001", event.getExternalReservationId());
        }

        @Test
        void shouldThrowExceptionWhenReleasingReservationWithoutExternalId() {
            InventoryTransaction transaction =
                    InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class, () -> transaction.releaseReservation());
            assertTrue(exception.getMessage().contains("no external reservation ID"));
        }

        @Test
        void shouldThrowExceptionWhenReleasingReservationInFailedStatus() {
            InventoryTransaction transaction =
                    InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);
            transaction.markAsProcessing();
            transaction.markAsReserved(ExternalReservationId.of("EXT-RES-001"));
            transaction.markAsProcessing();
            transaction.fail("Test failure");

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class, () -> transaction.releaseReservation());
            assertTrue(
                    exception.getMessage().contains("Cannot release reservation in FAILED status"));
        }
    }

    @Nested
    class clearDomainEventsMethodTest {
        @Test
        void shouldClearDomainEvents() {
            InventoryTransaction transaction =
                    InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);

            assertEquals(1, transaction.getDomainEvents().size());

            transaction.clearDomainEvents();

            assertEquals(0, transaction.getDomainEvents().size());
        }
    }

    @Nested
    class getTransactionLinesMethodTest {
        @Test
        void shouldReturnDefensiveCopyOfTransactionLines() {
            List<TransactionLine> lines = new ArrayList<>();
            lines.add(TransactionLine.of("SKU-001", 10));

            InventoryTransaction transaction =
                    InventoryTransaction.createInboundTransaction(
                            "PUTAWAY-001",
                            TransactionSource.PUTAWAY_TASK_COMPLETED,
                            "WH-01",
                            lines);

            List<TransactionLine> retrievedLines = transaction.getTransactionLines();
            retrievedLines.add(TransactionLine.of("SKU-002", 5));

            assertEquals(1, transaction.getTransactionLines().size());
        }
    }
}
