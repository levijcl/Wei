package com.wei.orchestrator.integration.inventory.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.command.*;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.inventory.domain.exception.InsufficientInventoryException;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.ExternalReservationId;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionStatus;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType;
import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class InventoryApplicationServiceIntegrationTest {

    @Autowired private InventoryApplicationService inventoryApplicationService;

    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;

    @MockitoBean private InventoryPort inventoryPort;

    @MockitoBean private ApplicationEventPublisher eventPublisher;

    @Nested
    class ReserveInventoryTests {

        @Test
        void shouldReserveInventoryAndPersistTransaction() {
            ReserveInventoryCommand command =
                    new ReserveInventoryCommand("ORDER-001", "SKU-001", "WH-01", 10);

            ExternalReservationId externalId = ExternalReservationId.of("EXT-RES-001");
            when(inventoryPort.createReservation("SKU-001", "WH-01", "ORDER-001", 10))
                    .thenReturn(externalId);

            InventoryOperationResultDto result =
                    inventoryApplicationService.reserveInventory(command, TriggerContext.manual());

            assertTrue(result.isSuccess());
            assertNotNull(result.getTransactionId());
            String transactionId = result.getTransactionId();

            Optional<InventoryTransaction> savedTransaction =
                    inventoryTransactionRepository.findById(transactionId);
            assertTrue(savedTransaction.isPresent());

            InventoryTransaction transaction = savedTransaction.get();
            assertEquals(TransactionType.OUTBOUND, transaction.getType());
            assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
            assertEquals("ORDER-001", transaction.getSourceReferenceId());
            assertEquals("EXT-RES-001", transaction.getExternalReservationId().getValue());
            assertEquals("WH-01", transaction.getWarehouseLocation().getWarehouseId());
            assertEquals(1, transaction.getTransactionLines().size());
            assertEquals("SKU-001", transaction.getTransactionLines().get(0).getSku());
            assertEquals(10, transaction.getTransactionLines().get(0).getQuantity());

            verify(inventoryPort).createReservation("SKU-001", "WH-01", "ORDER-001", 10);
        }

        @Test
        void shouldFailReservationWhenInsufficientInventory() {
            ReserveInventoryCommand command =
                    new ReserveInventoryCommand("ORDER-002", "SKU-002", "WH-01", 50);

            when(inventoryPort.createReservation("SKU-002", "WH-01", "ORDER-002", 50))
                    .thenThrow(
                            new InsufficientInventoryException(
                                    "Insufficient inventory for SKU: SKU-002"));

            InventoryOperationResultDto result =
                    inventoryApplicationService.reserveInventory(command, TriggerContext.manual());

            assertFalse(result.isSuccess());
            assertNotNull(result.getErrorMessage());
            assertTrue(
                    result.getErrorMessage().contains("Insufficient inventory for SKU: SKU-002"));

            List<InventoryTransaction> transactions =
                    inventoryTransactionRepository.findBySourceReferenceId("ORDER-002");
            assertFalse(transactions.isEmpty());

            InventoryTransaction transaction = transactions.get(0);
            assertEquals(TransactionStatus.FAILED, transaction.getStatus());
            assertEquals("ORDER-002", transaction.getSourceReferenceId());

            verify(inventoryPort).createReservation("SKU-002", "WH-01", "ORDER-002", 50);
        }

        @Test
        void shouldQueryReservationBySourceReferenceId() {
            ReserveInventoryCommand command =
                    new ReserveInventoryCommand("ORDER-003", "SKU-003", "WH-01", 15);

            ExternalReservationId externalId = ExternalReservationId.of("EXT-RES-003");
            when(inventoryPort.createReservation("SKU-003", "WH-01", "ORDER-003", 15))
                    .thenReturn(externalId);

            inventoryApplicationService.reserveInventory(command, TriggerContext.manual());

            List<InventoryTransaction> transactions =
                    inventoryTransactionRepository.findBySourceReferenceId("ORDER-003");

            assertFalse(transactions.isEmpty());
            assertEquals(1, transactions.size());
            assertEquals("ORDER-003", transactions.get(0).getSourceReferenceId());
            assertEquals("EXT-RES-003", transactions.get(0).getExternalReservationId().getValue());
        }
    }

    @Nested
    class ConsumeReservationTests {

        @Test
        void shouldConsumeReservationAndCreateNewTransaction() {
            ReserveInventoryCommand reserveCommand =
                    new ReserveInventoryCommand("ORDER-004", "SKU-004", "WH-01", 20);

            ExternalReservationId externalId = ExternalReservationId.of("EXT-RES-004");
            when(inventoryPort.createReservation("SKU-004", "WH-01", "ORDER-004", 20))
                    .thenReturn(externalId);

            InventoryOperationResultDto reserveResult =
                    inventoryApplicationService.reserveInventory(
                            reserveCommand, TriggerContext.manual());
            assertTrue(reserveResult.isSuccess());
            String reservationTransactionId = reserveResult.getTransactionId();

            doNothing().when(inventoryPort).consumeReservation(externalId);

            ConsumeReservationCommand consumeCommand =
                    new ConsumeReservationCommand(
                            reservationTransactionId, "EXT-RES-004", "ORDER-004");

            InventoryOperationResultDto consumeResult =
                    inventoryApplicationService.consumeReservation(
                            consumeCommand, TriggerContext.manual());

            assertTrue(consumeResult.isSuccess());
            assertNotNull(consumeResult.getTransactionId());
            String consumptionTransactionId = consumeResult.getTransactionId();

            Optional<InventoryTransaction> reservationTransaction =
                    inventoryTransactionRepository.findById(reservationTransactionId);
            assertTrue(reservationTransaction.isPresent());
            assertEquals(TransactionStatus.COMPLETED, reservationTransaction.get().getStatus());

            Optional<InventoryTransaction> consumptionTransaction =
                    inventoryTransactionRepository.findById(consumptionTransactionId);
            assertTrue(consumptionTransaction.isPresent());
            assertEquals(TransactionStatus.COMPLETED, consumptionTransaction.get().getStatus());
            assertEquals(TransactionType.OUTBOUND, consumptionTransaction.get().getType());
            assertEquals(
                    reservationTransactionId,
                    consumptionTransaction.get().getRelatedTransactionId());
            assertEquals("ORDER-004", consumptionTransaction.get().getSourceReferenceId());
            assertEquals(
                    "EXT-RES-004",
                    consumptionTransaction.get().getExternalReservationId().getValue());

            verify(inventoryPort).consumeReservation(externalId);
        }

        @Test
        void shouldThrowExceptionWhenTransactionNotFound() {
            ConsumeReservationCommand command =
                    new ConsumeReservationCommand("NON-EXISTENT", "EXT-RES-999", "ORDER-999");

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            inventoryApplicationService.consumeReservation(
                                    command, TriggerContext.manual()));

            verify(inventoryPort, never()).consumeReservation(any());
        }
    }

    @Nested
    class ReleaseReservationTests {

        @Test
        void shouldReleaseReservationAndUpdateTransaction() {
            ReserveInventoryCommand reserveCommand =
                    new ReserveInventoryCommand("ORDER-005", "SKU-005", "WH-01", 25);

            ExternalReservationId externalId = ExternalReservationId.of("EXT-RES-005");
            when(inventoryPort.createReservation("SKU-005", "WH-01", "ORDER-005", 25))
                    .thenReturn(externalId);

            InventoryOperationResultDto reserveResult =
                    inventoryApplicationService.reserveInventory(
                            reserveCommand, TriggerContext.manual());
            assertTrue(reserveResult.isSuccess());
            String transactionId = reserveResult.getTransactionId();

            doNothing().when(inventoryPort).releaseReservation(externalId);

            ReleaseReservationCommand releaseCommand =
                    new ReleaseReservationCommand(transactionId, "EXT-RES-005", "Order cancelled");

            InventoryOperationResultDto releaseResult =
                    inventoryApplicationService.releaseReservation(releaseCommand);
            assertTrue(releaseResult.isSuccess());

            Optional<InventoryTransaction> updatedTransaction =
                    inventoryTransactionRepository.findById(transactionId);
            assertTrue(updatedTransaction.isPresent());
            assertEquals(TransactionStatus.COMPLETED, updatedTransaction.get().getStatus());

            verify(inventoryPort).releaseReservation(externalId);
        }
    }

    @Nested
    class IncreaseInventoryTests {

        @Test
        void shouldIncreaseInventoryAndPersistTransaction() {
            IncreaseInventoryCommand command =
                    new IncreaseInventoryCommand(
                            "SKU-006", "WH-01", 30, "Putaway completed", "PUTAWAY-001");

            doNothing()
                    .when(inventoryPort)
                    .increaseInventory("SKU-006", "WH-01", 30, "Putaway completed");

            InventoryOperationResultDto result =
                    inventoryApplicationService.increaseInventory(command);

            assertTrue(result.isSuccess());
            assertNotNull(result.getTransactionId());
            String transactionId = result.getTransactionId();

            Optional<InventoryTransaction> savedTransaction =
                    inventoryTransactionRepository.findById(transactionId);
            assertTrue(savedTransaction.isPresent());

            InventoryTransaction transaction = savedTransaction.get();
            assertEquals(TransactionType.INBOUND, transaction.getType());
            assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
            assertEquals("PUTAWAY-001", transaction.getSourceReferenceId());
            assertEquals("WH-01", transaction.getWarehouseLocation().getWarehouseId());
            assertEquals(1, transaction.getTransactionLines().size());
            assertEquals("SKU-006", transaction.getTransactionLines().get(0).getSku());
            assertEquals(30, transaction.getTransactionLines().get(0).getQuantity());

            verify(inventoryPort).increaseInventory("SKU-006", "WH-01", 30, "Putaway completed");
        }

        @Test
        void shouldHandleExceptionDuringIncrease() {
            IncreaseInventoryCommand command =
                    new IncreaseInventoryCommand(
                            "SKU-007", "WH-01", 35, "Putaway completed", "PUTAWAY-002");

            doThrow(new RuntimeException("External system error"))
                    .when(inventoryPort)
                    .increaseInventory("SKU-007", "WH-01", 35, "Putaway completed");

            InventoryOperationResultDto result =
                    inventoryApplicationService.increaseInventory(command);

            assertFalse(result.isSuccess());
            assertNotNull(result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("External system error"));

            List<InventoryTransaction> transactions =
                    inventoryTransactionRepository.findBySourceReferenceId("PUTAWAY-002");
            assertFalse(transactions.isEmpty());

            InventoryTransaction transaction = transactions.get(0);
            assertEquals(TransactionStatus.FAILED, transaction.getStatus());
            assertEquals("PUTAWAY-002", transaction.getSourceReferenceId());

            verify(inventoryPort).increaseInventory("SKU-007", "WH-01", 35, "Putaway completed");
        }
    }

    @Nested
    class AdjustInventoryTests {

        @Test
        void shouldAdjustInventoryWithNegativeQuantity() {
            AdjustInventoryCommand command =
                    new AdjustInventoryCommand("SKU-008", "WH-01", -5, "Damaged goods", "ADJ-001");

            doNothing()
                    .when(inventoryPort)
                    .adjustInventory("SKU-008", "WH-01", -5, "Damaged goods");

            InventoryOperationResultDto result =
                    inventoryApplicationService.adjustInventory(command);

            assertTrue(result.isSuccess());
            assertNotNull(result.getTransactionId());
            String transactionId = result.getTransactionId();

            Optional<InventoryTransaction> savedTransaction =
                    inventoryTransactionRepository.findById(transactionId);
            assertTrue(savedTransaction.isPresent());

            InventoryTransaction transaction = savedTransaction.get();
            assertEquals(TransactionType.ADJUSTMENT, transaction.getType());
            assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
            assertEquals("ADJ-001", transaction.getSourceReferenceId());
            assertEquals(-5, transaction.getTransactionLines().get(0).getQuantity());

            verify(inventoryPort).adjustInventory("SKU-008", "WH-01", -5, "Damaged goods");
        }

        @Test
        void shouldAdjustInventoryWithPositiveQuantity() {
            AdjustInventoryCommand command =
                    new AdjustInventoryCommand(
                            "SKU-009", "WH-01", 10, "Found during cycle count", "ADJ-002");

            doNothing()
                    .when(inventoryPort)
                    .adjustInventory("SKU-009", "WH-01", 10, "Found during cycle count");

            InventoryOperationResultDto result =
                    inventoryApplicationService.adjustInventory(command);

            assertTrue(result.isSuccess());
            assertNotNull(result.getTransactionId());
            String transactionId = result.getTransactionId();

            Optional<InventoryTransaction> savedTransaction =
                    inventoryTransactionRepository.findById(transactionId);
            assertTrue(savedTransaction.isPresent());

            InventoryTransaction transaction = savedTransaction.get();
            assertEquals(TransactionType.ADJUSTMENT, transaction.getType());
            assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
            assertEquals(10, transaction.getTransactionLines().get(0).getQuantity());

            verify(inventoryPort)
                    .adjustInventory("SKU-009", "WH-01", 10, "Found during cycle count");
        }
    }
}
