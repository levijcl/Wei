package com.wei.orchestrator.inventory.application;

import com.wei.orchestrator.inventory.application.command.*;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.*;
import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryApplicationService {

    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryPort inventoryPort;
    private final ApplicationEventPublisher eventPublisher;

    public InventoryApplicationService(
            InventoryTransactionRepository inventoryTransactionRepository,
            InventoryPort inventoryPort,
            ApplicationEventPublisher eventPublisher) {
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryPort = inventoryPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public String reserveInventory(ReserveInventoryCommand command) {
        InventoryTransaction transaction =
                InventoryTransaction.createReservation(
                        command.getOrderId(),
                        command.getSku(),
                        command.getWarehouseId(),
                        command.getQuantity());

        inventoryTransactionRepository.save(transaction);

        try {
            ExternalReservationId externalReservationId =
                    inventoryPort.createReservation(
                            command.getSku(),
                            command.getWarehouseId(),
                            command.getOrderId(),
                            command.getQuantity());

            transaction.markAsReserved(externalReservationId);
            inventoryTransactionRepository.save(transaction);

            publishEvents(transaction);

            return transaction.getTransactionId();

        } catch (Exception e) {
            transaction.fail(e.getMessage());
            inventoryTransactionRepository.save(transaction);
            publishEvents(transaction);
            throw e;
        }
    }

    @Transactional
    public void consumeReservation(ConsumeReservationCommand command) {
        InventoryTransaction transaction =
                inventoryTransactionRepository
                        .findById(command.getTransactionId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Transaction not found: "
                                                        + command.getTransactionId()));

        if (transaction.getExternalReservationId() == null) {
            throw new IllegalStateException(
                    "Cannot consume reservation - no external reservation ID");
        }

        try {
            transaction.markAsProcessing();
            inventoryTransactionRepository.save(transaction);

            inventoryPort.consumeReservation(transaction.getExternalReservationId());

            transaction.complete();
            inventoryTransactionRepository.save(transaction);

            publishEvents(transaction);

        } catch (Exception e) {
            transaction.fail(e.getMessage());
            inventoryTransactionRepository.save(transaction);
            publishEvents(transaction);
            throw e;
        }
    }

    @Transactional
    public void releaseReservation(ReleaseReservationCommand command) {
        InventoryTransaction transaction =
                inventoryTransactionRepository
                        .findById(command.getTransactionId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Transaction not found: "
                                                        + command.getTransactionId()));

        if (transaction.getExternalReservationId() == null) {
            throw new IllegalStateException(
                    "Cannot release reservation - no external reservation ID");
        }

        try {
            inventoryPort.releaseReservation(transaction.getExternalReservationId());

            transaction.releaseReservation();
            inventoryTransactionRepository.save(transaction);

            publishEvents(transaction);

        } catch (Exception e) {
            transaction.fail(e.getMessage());
            inventoryTransactionRepository.save(transaction);
            publishEvents(transaction);
            throw e;
        }
    }

    @Transactional
    public String increaseInventory(IncreaseInventoryCommand command) {
        List<TransactionLine> lines = new ArrayList<>();
        lines.add(TransactionLine.of(command.getSku(), command.getQuantity()));

        InventoryTransaction transaction =
                InventoryTransaction.createInboundTransaction(
                        command.getSourceReferenceId(),
                        TransactionSource.PUTAWAY_TASK_COMPLETED,
                        command.getWarehouseId(),
                        lines);

        inventoryTransactionRepository.save(transaction);

        try {
            transaction.markAsProcessing();
            inventoryTransactionRepository.save(transaction);

            inventoryPort.increaseInventory(
                    command.getSku(),
                    command.getWarehouseId(),
                    command.getQuantity(),
                    command.getReason() != null
                            ? command.getReason()
                            : "Putaway completed: " + command.getSourceReferenceId());

            transaction.complete();
            inventoryTransactionRepository.save(transaction);

            publishEvents(transaction);

            return transaction.getTransactionId();

        } catch (Exception e) {
            transaction.fail(e.getMessage());
            inventoryTransactionRepository.save(transaction);
            publishEvents(transaction);
            throw e;
        }
    }

    @Transactional
    public String adjustInventory(AdjustInventoryCommand command) {
        List<TransactionLine> lines = new ArrayList<>();
        lines.add(TransactionLine.of(command.getSku(), command.getQuantityChange()));

        InventoryTransaction transaction =
                InventoryTransaction.createAdjustmentTransaction(
                        command.getSourceReferenceId(),
                        TransactionSource.MANUAL_ADJUSTMENT,
                        command.getWarehouseId(),
                        lines);

        inventoryTransactionRepository.save(transaction);

        try {
            transaction.markAsProcessing();
            inventoryTransactionRepository.save(transaction);

            inventoryPort.adjustInventory(
                    command.getSku(),
                    command.getWarehouseId(),
                    command.getQuantityChange(),
                    command.getReason() != null
                            ? command.getReason()
                            : "Manual adjustment: " + command.getSourceReferenceId());

            transaction.complete();
            inventoryTransactionRepository.save(transaction);

            publishEvents(transaction);

            return transaction.getTransactionId();

        } catch (Exception e) {
            transaction.fail(e.getMessage());
            inventoryTransactionRepository.save(transaction);
            publishEvents(transaction);
            throw e;
        }
    }

    private void publishEvents(InventoryTransaction transaction) {
        transaction.getDomainEvents().forEach(eventPublisher::publishEvent);
        transaction.clearDomainEvents();
    }
}
