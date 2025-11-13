package com.wei.orchestrator.unit.inventory.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.command.*;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.inventory.domain.exception.InsufficientInventoryException;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.ExternalReservationId;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionStatus;
import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InventoryApplicationServiceTest {

    @Mock private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock private InventoryPort inventoryPort;

    @Mock private ApplicationEventPublisher eventPublisher;

    private InventoryApplicationService inventoryApplicationService;

    @BeforeEach
    void setUp() {
        inventoryApplicationService =
                new InventoryApplicationService(
                        inventoryTransactionRepository, inventoryPort, eventPublisher);
    }

    @Nested
    class reserveInventoryMethodTest {
        @Test
        void shouldReserveInventorySuccessfully() {
            ReserveInventoryCommand command =
                    new ReserveInventoryCommand("ORDER-001", "SKU-001", "WH-01", 10);

            ExternalReservationId externalId = ExternalReservationId.of("EXT-RES-001");

            when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(inventoryPort.createReservation("SKU-001", "WH-01", "ORDER-001", 10))
                    .thenReturn(externalId);

            InventoryOperationResultDto result =
                    inventoryApplicationService.reserveInventory(command);

            assertTrue(result.isSuccess());
            assertNotNull(result.getTransactionId());
            verify(inventoryPort).createReservation("SKU-001", "WH-01", "ORDER-001", 10);
            verify(inventoryTransactionRepository, atLeast(2))
                    .save(any(InventoryTransaction.class));
            verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
        }

        @Test
        void shouldFailReservationWhenInsufficientInventory() {
            ReserveInventoryCommand command =
                    new ReserveInventoryCommand("ORDER-002", "SKU-002", "WH-01", 50);

            when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(inventoryPort.createReservation("SKU-002", "WH-01", "ORDER-002", 50))
                    .thenThrow(new InsufficientInventoryException("Insufficient inventory"));

            InventoryOperationResultDto result =
                    inventoryApplicationService.reserveInventory(command);

            assertFalse(result.isSuccess());
            assertNotNull(result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("Insufficient inventory"));

            verify(inventoryPort).createReservation("SKU-002", "WH-01", "ORDER-002", 50);
            verify(inventoryTransactionRepository, atLeast(2))
                    .save(any(InventoryTransaction.class));
            verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
        }
    }

    @Nested
    class consumeReservationMethodTest {
        @Test
        void shouldConsumeReservationSuccessfully() {
            InventoryTransaction reservationTransaction =
                    InventoryTransaction.createReservation("ORDER-001", "SKU-001", "WH-01", 10);
            reservationTransaction.markAsReserved(ExternalReservationId.of("EXT-RES-001"));

            ConsumeReservationCommand command =
                    new ConsumeReservationCommand(
                            reservationTransaction.getTransactionId(), "EXT-RES-001", "ORDER-001");

            when(inventoryTransactionRepository.findById(reservationTransaction.getTransactionId()))
                    .thenReturn(Optional.of(reservationTransaction));
            when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            InventoryOperationResultDto result =
                    inventoryApplicationService.consumeReservation(command);

            assertTrue(result.isSuccess());
            assertNotNull(result.getTransactionId());
            verify(inventoryPort).consumeReservation(ExternalReservationId.of("EXT-RES-001"));
            verify(inventoryTransactionRepository, atLeast(3))
                    .save(any(InventoryTransaction.class));
            verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
        }

        @Test
        void shouldThrowExceptionWhenTransactionNotFoundForConsume() {
            ConsumeReservationCommand command =
                    new ConsumeReservationCommand("NON-EXISTENT", "EXT-RES-001", "ORDER-001");

            when(inventoryTransactionRepository.findById("NON-EXISTENT"))
                    .thenReturn(Optional.empty());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> inventoryApplicationService.consumeReservation(command));

            verify(inventoryPort, never()).consumeReservation(any());
        }

        @Test
        void shouldFailWhenReservationTransactionNotCompleted() {
            InventoryTransaction reservationTransaction =
                    InventoryTransaction.createReservation("ORDER-002", "SKU-002", "WH-01", 10);

            ConsumeReservationCommand command =
                    new ConsumeReservationCommand(
                            reservationTransaction.getTransactionId(), "EXT-RES-002", "ORDER-002");

            when(inventoryTransactionRepository.findById(reservationTransaction.getTransactionId()))
                    .thenReturn(Optional.of(reservationTransaction));

            InventoryOperationResultDto result =
                    inventoryApplicationService.consumeReservation(command);

            assertFalse(result.isSuccess());
            assertEquals(
                    "Cannot consume reservation - reservation transaction is not completed",
                    result.getErrorMessage());
            verify(inventoryPort, never()).consumeReservation(any());
        }
    }

    @Nested
    class releaseReservationMethodTest {
        @Test
        void shouldReleaseReservationSuccessfully() {
            InventoryTransaction transaction =
                    InventoryTransaction.createReservation("ORDER-003", "SKU-003", "WH-01", 15);
            transaction.markAsReserved(ExternalReservationId.of("EXT-RES-003"));
            transaction.setStatus(TransactionStatus.PENDING);

            ReleaseReservationCommand command =
                    new ReleaseReservationCommand(
                            transaction.getTransactionId(), "EXT-RES-003", "Order cancelled");

            when(inventoryTransactionRepository.findById(transaction.getTransactionId()))
                    .thenReturn(Optional.of(transaction));
            when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            inventoryApplicationService.releaseReservation(command);

            verify(inventoryPort).releaseReservation(ExternalReservationId.of("EXT-RES-003"));
            verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
            verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
        }
    }

    @Nested
    class increaseInventoryMethodTest {
        @Test
        void shouldIncreaseInventorySuccessfully() {
            IncreaseInventoryCommand command =
                    new IncreaseInventoryCommand(
                            "SKU-004", "WH-01", 20, "Putaway completed", "PUTAWAY-001");

            when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            InventoryOperationResultDto result =
                    inventoryApplicationService.increaseInventory(command);

            assertTrue(result.isSuccess());
            assertNotNull(result.getTransactionId());
            verify(inventoryPort).increaseInventory("SKU-004", "WH-01", 20, "Putaway completed");
            verify(inventoryTransactionRepository, atLeast(2))
                    .save(any(InventoryTransaction.class));
            verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
        }

        @Test
        void shouldHandleExceptionDuringIncrease() {
            IncreaseInventoryCommand command =
                    new IncreaseInventoryCommand(
                            "SKU-006", "WH-01", 25, "Putaway completed", "PUTAWAY-002");

            when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            doThrow(new RuntimeException("External system error"))
                    .when(inventoryPort)
                    .increaseInventory(anyString(), anyString(), anyInt(), anyString());

            InventoryOperationResultDto result =
                    inventoryApplicationService.increaseInventory(command);

            assertFalse(result.isSuccess());
            assertNotNull(result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("External system error"));

            verify(inventoryPort).increaseInventory("SKU-006", "WH-01", 25, "Putaway completed");
            verify(inventoryTransactionRepository, atLeast(2))
                    .save(any(InventoryTransaction.class));
            verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
        }
    }

    @Nested
    class adjustInventoryMethodTests {
        @Test
        void shouldAdjustInventorySuccessfully() {
            AdjustInventoryCommand command =
                    new AdjustInventoryCommand("SKU-005", "WH-01", -3, "Damaged goods", "ADJ-001");

            when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            InventoryOperationResultDto result =
                    inventoryApplicationService.adjustInventory(command);

            assertTrue(result.isSuccess());
            assertNotNull(result.getTransactionId());
            verify(inventoryPort).adjustInventory("SKU-005", "WH-01", -3, "Damaged goods");
            verify(inventoryTransactionRepository, atLeast(2))
                    .save(any(InventoryTransaction.class));
            verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
        }
    }
}
