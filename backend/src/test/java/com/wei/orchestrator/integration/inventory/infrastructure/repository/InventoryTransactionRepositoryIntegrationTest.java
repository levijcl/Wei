package com.wei.orchestrator.integration.inventory.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.*;
import com.wei.orchestrator.inventory.infrastructure.repository.InventoryTransactionRepositoryImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import(InventoryTransactionRepositoryImpl.class)
class InventoryTransactionRepositoryIntegrationTest {

    @Autowired private InventoryTransactionRepositoryImpl repository;

    @Test
    void shouldSaveAndFindReservationTransactionById() {
        InventoryTransaction transaction =
                InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);

        InventoryTransaction savedTransaction = repository.save(transaction);

        assertNotNull(savedTransaction);
        assertEquals(transaction.getTransactionId(), savedTransaction.getTransactionId());

        Optional<InventoryTransaction> found =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(found.isPresent());
        assertEquals("ORDER-001", found.get().getSourceReferenceId());
        assertEquals(TransactionType.OUTBOUND, found.get().getType());
        assertEquals(TransactionStatus.PENDING, found.get().getStatus());
        assertEquals(TransactionSource.ORDER_RESERVATION, found.get().getSource());
        assertEquals("WH-01", found.get().getWarehouseLocation().getWarehouseId());
        assertEquals(1, found.get().getTransactionLines().size());
        assertEquals("SKU-001", found.get().getTransactionLines().get(0).getSku());
        assertEquals(10, found.get().getTransactionLines().get(0).getQuantity());
    }

    @Test
    void shouldSaveAndFindInboundTransactionWithMultipleLines() {
        List<TransactionLine> lines = new ArrayList<>();
        lines.add(TransactionLine.of("SKU-001", 10));
        lines.add(TransactionLine.of("SKU-002", 20));
        lines.add(TransactionLine.of("SKU-003", 5));

        InventoryTransaction transaction =
                InventoryTransaction.createInboundTransaction(
                        "PUTAWAY-001", TransactionSource.PUTAWAY_TASK_COMPLETED, "WH-02", lines);

        InventoryTransaction savedTransaction = repository.save(transaction);

        Optional<InventoryTransaction> found =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(found.isPresent());
        assertEquals(3, found.get().getTransactionLines().size());
        assertEquals(TransactionType.INBOUND, found.get().getType());
    }

    @Test
    void shouldSaveAndFindOutboundTransactionWithExternalReservationId() {
        List<TransactionLine> lines = new ArrayList<>();
        lines.add(TransactionLine.of("SKU-001", 15));

        InventoryTransaction transaction =
                InventoryTransaction.createOutboundTransaction(
                        "PICKING-001",
                        TransactionSource.PICKING_TASK_COMPLETED,
                        "WH-01",
                        lines,
                        ExternalReservationId.of("EXT-RES-001"));

        InventoryTransaction savedTransaction = repository.save(transaction);

        Optional<InventoryTransaction> found =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(found.isPresent());
        assertNotNull(found.get().getExternalReservationId());
        assertEquals("EXT-RES-001", found.get().getExternalReservationId().getValue());
    }

    @Test
    void shouldSaveCompleteTransactionLifecycle() {
        InventoryTransaction transaction =
                InventoryTransaction.createReservation("ORDER-002", "SKU-002", "WH-01", 25);
        InventoryTransaction savedTransaction = repository.save(transaction);

        Optional<InventoryTransaction> pendingTransaction =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(pendingTransaction.isPresent());
        assertEquals(TransactionStatus.PENDING, pendingTransaction.get().getStatus());

        InventoryTransaction transactionToReserve = pendingTransaction.get();
        transactionToReserve.markAsReserved(ExternalReservationId.of("EXT-RES-002"));
        repository.save(transactionToReserve);

        Optional<InventoryTransaction> reservedTransaction =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(reservedTransaction.isPresent());
        assertEquals(TransactionStatus.COMPLETED, reservedTransaction.get().getStatus());
        assertEquals(
                "EXT-RES-002", reservedTransaction.get().getExternalReservationId().getValue());
        assertNotNull(reservedTransaction.get().getCompletedAt());
    }

    @Test
    void shouldSaveFailedTransaction() {
        InventoryTransaction transaction =
                InventoryTransaction.createReservation("ORDER-003", "SKU-003", "WH-01", 30);
        InventoryTransaction savedTransaction = repository.save(transaction);

        InventoryTransaction transactionToFail = savedTransaction;
        transactionToFail.fail("Insufficient inventory");
        repository.save(transactionToFail);

        Optional<InventoryTransaction> failedTransaction =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(failedTransaction.isPresent());
        assertEquals(TransactionStatus.FAILED, failedTransaction.get().getStatus());
        assertEquals("Insufficient inventory", failedTransaction.get().getFailureReason());
        assertNotNull(failedTransaction.get().getCompletedAt());
    }

    @Test
    void shouldFindTransactionsBySourceReferenceId() {
        InventoryTransaction transaction1 =
                InventoryTransaction.createReservation("ORDER-004", "SKU-001", "WH-01", 10);
        repository.save(transaction1);

        List<TransactionLine> lines = new ArrayList<>();
        lines.add(TransactionLine.of("SKU-002", 5));
        InventoryTransaction transaction2 =
                InventoryTransaction.createOutboundTransaction(
                        "ORDER-004",
                        TransactionSource.RESERVATION_CONSUMED,
                        "WH-01",
                        lines,
                        ExternalReservationId.of("EXT-RES-004"));
        repository.save(transaction2);

        List<InventoryTransaction> found = repository.findBySourceReferenceId("ORDER-004");
        assertEquals(2, found.size());
    }

    @Test
    void shouldUpdateTransactionLines() {
        List<TransactionLine> initialLines = new ArrayList<>();
        initialLines.add(TransactionLine.of("SKU-001", 10));

        InventoryTransaction transaction =
                InventoryTransaction.createInboundTransaction(
                        "PUTAWAY-006",
                        TransactionSource.PUTAWAY_TASK_COMPLETED,
                        "WH-01",
                        initialLines);
        InventoryTransaction savedTransaction = repository.save(transaction);

        Optional<InventoryTransaction> found =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(found.isPresent());
        assertEquals(1, found.get().getTransactionLines().size());

        List<TransactionLine> updatedLines = new ArrayList<>();
        updatedLines.add(TransactionLine.of("SKU-001", 10));
        updatedLines.add(TransactionLine.of("SKU-002", 20));
        found.get().setTransactionLines(updatedLines);
        repository.save(found.get());

        Optional<InventoryTransaction> updated =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(updated.isPresent());
        assertEquals(2, updated.get().getTransactionLines().size());
    }

    @Test
    void shouldReturnEmptyWhenTransactionNotFound() {
        Optional<InventoryTransaction> notFound = repository.findById("NON-EXISTENT-ID");
        assertFalse(notFound.isPresent());
    }

    @Test
    void shouldSaveAdjustmentTransactionWithNegativeQuantity() {
        List<TransactionLine> lines = new ArrayList<>();
        lines.add(TransactionLine.of("SKU-001", -5));
        lines.add(TransactionLine.of("SKU-002", 3));

        InventoryTransaction transaction =
                InventoryTransaction.createAdjustmentTransaction(
                        "ADJ-001", TransactionSource.CYCLE_COUNT_ADJUSTMENT, "WH-01", lines);
        InventoryTransaction savedTransaction = repository.save(transaction);

        Optional<InventoryTransaction> found =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(found.isPresent());
        assertEquals(TransactionType.ADJUSTMENT, found.get().getType());
        assertEquals(2, found.get().getTransactionLines().size());
        assertEquals(-5, found.get().getTransactionLines().get(0).getQuantity());
        assertEquals(3, found.get().getTransactionLines().get(1).getQuantity());
    }

    @Test
    void shouldSaveTransactionWithZone() {
        List<TransactionLine> lines = new ArrayList<>();
        lines.add(TransactionLine.of("SKU-001", 10));

        InventoryTransaction transaction =
                InventoryTransaction.createInboundTransaction(
                        "PUTAWAY-007", TransactionSource.PUTAWAY_TASK_COMPLETED, "WH-01", lines);
        transaction.setWarehouseLocation(WarehouseLocation.of("WH-01", "ZONE-A"));
        InventoryTransaction savedTransaction = repository.save(transaction);

        Optional<InventoryTransaction> found =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(found.isPresent());
        assertEquals("WH-01", found.get().getWarehouseLocation().getWarehouseId());
        assertEquals("ZONE-A", found.get().getWarehouseLocation().getZone());
        assertTrue(found.get().getWarehouseLocation().hasZone());
    }

    @Test
    void shouldHandleProcessingToCompletedLifecycle() {
        List<TransactionLine> lines = new ArrayList<>();
        lines.add(TransactionLine.of("SKU-001", 10));

        InventoryTransaction transaction =
                InventoryTransaction.createInboundTransaction(
                        "PUTAWAY-008", TransactionSource.PUTAWAY_TASK_COMPLETED, "WH-01", lines);
        InventoryTransaction savedTransaction = repository.save(transaction);

        InventoryTransaction toProcess = savedTransaction;
        toProcess.markAsProcessing();
        repository.save(toProcess);

        Optional<InventoryTransaction> processing =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(processing.isPresent());
        assertEquals(TransactionStatus.PROCESSING, processing.get().getStatus());

        InventoryTransaction toComplete = processing.get();
        toComplete.complete();
        repository.save(toComplete);

        Optional<InventoryTransaction> completed =
                repository.findById(savedTransaction.getTransactionId());
        assertTrue(completed.isPresent());
        assertEquals(TransactionStatus.COMPLETED, completed.get().getStatus());
        assertNotNull(completed.get().getCompletedAt());
    }
}
