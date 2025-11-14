package com.wei.orchestrator.unit.inventory.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.inventory.application.InventoryAdjustmentApplicationService;
import com.wei.orchestrator.inventory.application.command.ApplyAdjustmentCommand;
import com.wei.orchestrator.inventory.application.command.DetectDiscrepancyCommand;
import com.wei.orchestrator.inventory.domain.model.InventoryAdjustment;
import com.wei.orchestrator.inventory.domain.model.valueobject.DiscrepancyLog;
import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.domain.repository.InventoryAdjustmentRepository;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import com.wei.orchestrator.observation.domain.model.valueobject.StockSnapshot;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.infrastructure.adapter.dto.WesInventoryDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InventoryAdjustmentApplicationServiceTest {

    @Mock private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Mock private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock private WesPort wesPort;

    @Mock private InventoryPort inventoryPort;

    @Mock private ApplicationEventPublisher eventPublisher;

    private InventoryAdjustmentApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new InventoryAdjustmentApplicationService(
                        inventoryAdjustmentRepository,
                        inventoryTransactionRepository,
                        wesPort,
                        inventoryPort,
                        eventPublisher);
    }

    @Nested
    class DetectDiscrepancyMethodTest {
        @Test
        void shouldDetectDiscrepancyAndSaveAdjustment() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            DetectDiscrepancyCommand command =
                    new DetectDiscrepancyCommand("OBSERVER-001", inventorySnapshots);

            when(wesPort.getInventorySnapshot()).thenReturn(new ArrayList<>());
            when(inventoryAdjustmentRepository.save(any(InventoryAdjustment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            String adjustmentId = service.detectDiscrepancy(command);

            assertNotNull(adjustmentId);
            verify(wesPort).getInventorySnapshot();
            verify(inventoryAdjustmentRepository).save(any(InventoryAdjustment.class));
        }

        @Test
        void shouldReturnAdjustmentIdWhenDiscrepanciesFound() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            DetectDiscrepancyCommand command =
                    new DetectDiscrepancyCommand("OBSERVER-001", inventorySnapshots);

            List<WesInventoryDto> wesInventory = new ArrayList<>();
            when(wesPort.getInventorySnapshot()).thenReturn(wesInventory);

            when(inventoryAdjustmentRepository.save(any(InventoryAdjustment.class)))
                    .thenAnswer(
                            invocation -> {
                                InventoryAdjustment adj = invocation.getArgument(0);
                                List<DiscrepancyLog> logs = new ArrayList<>();
                                logs.add(DiscrepancyLog.of("SKU-001", "WH-01", 15, 10));
                                adj.setDiscrepancyLogs(logs);
                                return adj;
                            });

            service.detectDiscrepancy(command);

            verify(inventoryAdjustmentRepository).save(any(InventoryAdjustment.class));
        }

        @Test
        void shouldPublishEventsWhenDiscrepanciesFound() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            DetectDiscrepancyCommand command =
                    new DetectDiscrepancyCommand("OBSERVER-001", inventorySnapshots);

            when(wesPort.getInventorySnapshot()).thenReturn(new ArrayList<>());
            when(inventoryAdjustmentRepository.save(any(InventoryAdjustment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.detectDiscrepancy(command);

            verify(inventoryAdjustmentRepository).save(any(InventoryAdjustment.class));
        }

        @Test
        void shouldHandleExceptionWhenWesPortFails() {
            List<StockSnapshot> inventorySnapshots = new ArrayList<>();
            inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

            DetectDiscrepancyCommand command =
                    new DetectDiscrepancyCommand("OBSERVER-001", inventorySnapshots);

            when(wesPort.getInventorySnapshot())
                    .thenThrow(new RuntimeException("WES connection failed"));

            RuntimeException exception =
                    assertThrows(RuntimeException.class, () -> service.detectDiscrepancy(command));

            assertTrue(
                    exception
                            .getMessage()
                            .contains(
                                    "Failed to fetch WES inventory snapshot: WES connection"
                                            + " failed"));
            verify(wesPort).getInventorySnapshot();
        }
    }

    @Nested
    class ApplyAdjustmentMethodTest {
        @Test
        void shouldApplyAdjustmentSuccessfully() {
            ApplyAdjustmentCommand command = new ApplyAdjustmentCommand("ADJ-001");

            InventoryAdjustment adjustment = createAdjustmentWithDiscrepancies();
            when(inventoryAdjustmentRepository.findById("ADJ-001"))
                    .thenReturn(Optional.of(adjustment));
            when(inventoryAdjustmentRepository.save(any(InventoryAdjustment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(inventoryTransactionRepository.save(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.applyAdjustment(command);

            verify(inventoryAdjustmentRepository).findById("ADJ-001");
            verify(inventoryTransactionRepository, atLeastOnce()).save(any());
            verify(inventoryPort, atLeastOnce()).adjustInventory(any(), any(), anyInt(), any());
            verify(inventoryAdjustmentRepository, atLeast(1)).save(any(InventoryAdjustment.class));
        }

        @Test
        void shouldThrowExceptionWhenAdjustmentNotFound() {
            ApplyAdjustmentCommand command = new ApplyAdjustmentCommand("ADJ-999");

            when(inventoryAdjustmentRepository.findById("ADJ-999")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class, () -> service.applyAdjustment(command));

            assertTrue(exception.getMessage().contains("Adjustment not found"));
            verify(inventoryAdjustmentRepository).findById("ADJ-999");
        }

        @Test
        void shouldNotApplyWhenNoDiscrepancies() {
            ApplyAdjustmentCommand command = new ApplyAdjustmentCommand("ADJ-002");

            InventoryAdjustment adjustment = createAdjustmentWithoutDiscrepancies();
            when(inventoryAdjustmentRepository.findById("ADJ-002"))
                    .thenReturn(Optional.of(adjustment));

            service.applyAdjustment(command);

            verify(inventoryAdjustmentRepository).findById("ADJ-002");
            verify(inventoryTransactionRepository, never()).save(any());
            verify(inventoryPort, never()).adjustInventory(any(), any(), anyInt(), any());
        }

        @Test
        void shouldHandleMultipleWarehouses() {
            ApplyAdjustmentCommand command = new ApplyAdjustmentCommand("ADJ-003");

            InventoryAdjustment adjustment = createAdjustmentWithMultipleWarehouses();
            when(inventoryAdjustmentRepository.findById("ADJ-003"))
                    .thenReturn(Optional.of(adjustment));
            when(inventoryAdjustmentRepository.save(any(InventoryAdjustment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(inventoryTransactionRepository.save(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.applyAdjustment(command);

            verify(inventoryTransactionRepository, atLeast(2)).save(any());
        }

        @Test
        void shouldHandleExceptionAndFailAdjustment() {
            ApplyAdjustmentCommand command = new ApplyAdjustmentCommand("ADJ-004");

            InventoryAdjustment adjustment = createAdjustmentWithDiscrepancies();
            when(inventoryAdjustmentRepository.findById("ADJ-004"))
                    .thenReturn(Optional.of(adjustment));
            when(inventoryTransactionRepository.save(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            doThrow(new RuntimeException("Inventory port error"))
                    .when(inventoryPort)
                    .adjustInventory(any(), any(), anyInt(), any());
            when(inventoryAdjustmentRepository.save(any(InventoryAdjustment.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RuntimeException exception =
                    assertThrows(RuntimeException.class, () -> service.applyAdjustment(command));

            assertTrue(exception.getMessage().contains("Failed to apply adjustment"));
            verify(inventoryAdjustmentRepository, atLeast(1)).save(any(InventoryAdjustment.class));
        }
    }

    private InventoryAdjustment createAdjustmentWithDiscrepancies() {
        List<StockSnapshot> inventorySnapshots = new ArrayList<>();
        inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

        List<StockSnapshot> wesSnapshots = new ArrayList<>();
        wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));

        return InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);
    }

    private InventoryAdjustment createAdjustmentWithoutDiscrepancies() {
        List<StockSnapshot> inventorySnapshots = new ArrayList<>();
        inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

        List<StockSnapshot> wesSnapshots = new ArrayList<>();
        wesSnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));

        return InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);
    }

    private InventoryAdjustment createAdjustmentWithMultipleWarehouses() {
        List<StockSnapshot> inventorySnapshots = new ArrayList<>();
        inventorySnapshots.add(new StockSnapshot("SKU-001", 10, "WH-01", LocalDateTime.now()));
        inventorySnapshots.add(new StockSnapshot("SKU-002", 20, "WH-02", LocalDateTime.now()));

        List<StockSnapshot> wesSnapshots = new ArrayList<>();
        wesSnapshots.add(new StockSnapshot("SKU-001", 15, "WH-01", LocalDateTime.now()));
        wesSnapshots.add(new StockSnapshot("SKU-002", 25, "WH-02", LocalDateTime.now()));

        return InventoryAdjustment.detectDiscrepancy(inventorySnapshots, wesSnapshots);
    }
}
