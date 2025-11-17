package com.wei.orchestrator.inventory.application;

import com.wei.orchestrator.inventory.application.command.*;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.inventory.domain.event.InventoryReservedEvent;
import com.wei.orchestrator.inventory.domain.event.InventoryTransactionFailedEvent;
import com.wei.orchestrator.inventory.domain.event.ReservationFailedEvent;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.*;
import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
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
    public InventoryOperationResultDto reserveInventory(
            ReserveInventoryCommand command, TriggerContext triggerContext) {
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

            publishEventsWithContext(transaction, triggerContext);

            return InventoryOperationResultDto.success(transaction.getTransactionId());

        } catch (Exception e) {
            transaction.fail(e.getMessage());
            inventoryTransactionRepository.save(transaction);
            publishEventsWithContext(transaction, triggerContext);
            return InventoryOperationResultDto.failure(e.getMessage());
        }
    }

    @Transactional
    public InventoryOperationResultDto consumeReservation(ConsumeReservationCommand command) {
        InventoryTransaction reservationTransaction =
                inventoryTransactionRepository
                        .findById(command.getTransactionId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Reservation transaction not found: "
                                                        + command.getTransactionId()));

        if (reservationTransaction.getStatus() != TransactionStatus.COMPLETED) {
            return InventoryOperationResultDto.failure(
                    "Cannot consume reservation - reservation transaction is not completed");
        }

        if (reservationTransaction.getExternalReservationId() == null) {
            return InventoryOperationResultDto.failure(
                    "Cannot consume reservation - no external reservation ID");
        }

        InventoryTransaction consumptionTransaction =
                InventoryTransaction.createConsumptionTransaction(
                        command.getSourceReferenceId(),
                        reservationTransaction.getTransactionId(),
                        reservationTransaction.getExternalReservationId(),
                        reservationTransaction.getWarehouseLocation().getWarehouseId(),
                        reservationTransaction.getTransactionLines());

        inventoryTransactionRepository.save(consumptionTransaction);

        try {
            consumptionTransaction.markAsProcessing();
            inventoryTransactionRepository.save(consumptionTransaction);

            inventoryPort.consumeReservation(consumptionTransaction.getExternalReservationId());

            consumptionTransaction.complete();
            inventoryTransactionRepository.save(consumptionTransaction);

            publishEvents(consumptionTransaction);

            return InventoryOperationResultDto.success(consumptionTransaction.getTransactionId());

        } catch (Exception e) {
            consumptionTransaction.fail(e.getMessage());
            inventoryTransactionRepository.save(consumptionTransaction);
            publishEvents(consumptionTransaction);
            return InventoryOperationResultDto.failure(e.getMessage());
        }
    }

    @Transactional
    public InventoryOperationResultDto consumeReservationForOrder(String orderId) {
        List<InventoryTransaction> transactions =
                inventoryTransactionRepository.findBySourceReferenceId(orderId);

        InventoryTransaction reservationTransaction =
                transactions.stream()
                        .filter(t -> t.getExternalReservationId() != null)
                        .filter(t -> t.getStatus().equals(TransactionStatus.COMPLETED))
                        .filter(t -> t.getSource().equals(TransactionSource.ORDER_RESERVATION))
                        .findFirst()
                        .orElse(null);

        if (reservationTransaction == null) {
            return InventoryOperationResultDto.failure(
                    "No valid reservation transaction found for order: " + orderId);
        }

        ConsumeReservationCommand command =
                new ConsumeReservationCommand(
                        reservationTransaction.getTransactionId(),
                        reservationTransaction.getExternalReservationId().getValue(),
                        orderId);

        return consumeReservation(command);
    }

    @Transactional
    public InventoryOperationResultDto releaseReservationForOrder(String orderId, String reason) {
        List<InventoryTransaction> transactions =
                inventoryTransactionRepository.findBySourceReferenceId(orderId);

        InventoryTransaction reservationTransaction =
                transactions.stream()
                        .filter(t -> t.getExternalReservationId() != null)
                        .filter(t -> t.getStatus().equals(TransactionStatus.COMPLETED))
                        .filter(t -> t.getSource().equals(TransactionSource.ORDER_RESERVATION))
                        .findFirst()
                        .orElse(null);

        if (reservationTransaction == null) {
            return InventoryOperationResultDto.failure(
                    "No valid reservation transaction found for order: " + orderId);
        }

        ReleaseReservationCommand command =
                new ReleaseReservationCommand(
                        reservationTransaction.getTransactionId(),
                        reservationTransaction.getExternalReservationId().getValue(),
                        reason);

        return releaseReservation(command);
    }

    @Transactional
    public InventoryOperationResultDto releaseReservation(ReleaseReservationCommand command) {
        InventoryTransaction transaction =
                inventoryTransactionRepository
                        .findById(command.getTransactionId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Transaction not found: "
                                                        + command.getTransactionId()));

        if (transaction.getExternalReservationId() == null) {
            return InventoryOperationResultDto.failure(
                    "Cannot release reservation - no external reservation ID");
        }

        try {
            inventoryPort.releaseReservation(transaction.getExternalReservationId());

            transaction.releaseReservation();
            inventoryTransactionRepository.save(transaction);

            publishEvents(transaction);

            return InventoryOperationResultDto.successVoid();

        } catch (Exception e) {
            transaction.fail(e.getMessage());
            inventoryTransactionRepository.save(transaction);
            publishEvents(transaction);
            return InventoryOperationResultDto.failure(e.getMessage());
        }
    }

    @Transactional
    public InventoryOperationResultDto increaseInventory(IncreaseInventoryCommand command) {
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

            return InventoryOperationResultDto.success(transaction.getTransactionId());

        } catch (Exception e) {
            transaction.fail(e.getMessage());
            inventoryTransactionRepository.save(transaction);
            publishEvents(transaction);
            return InventoryOperationResultDto.failure(e.getMessage());
        }
    }

    @Transactional
    public InventoryOperationResultDto adjustInventory(AdjustInventoryCommand command) {
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

            return InventoryOperationResultDto.success(transaction.getTransactionId());

        } catch (Exception e) {
            transaction.fail(e.getMessage());
            inventoryTransactionRepository.save(transaction);
            publishEvents(transaction);
            return InventoryOperationResultDto.failure(e.getMessage());
        }
    }

    private void publishEvents(InventoryTransaction transaction) {
        transaction.getDomainEvents().forEach(eventPublisher::publishEvent);
        transaction.clearDomainEvents();
    }

    private void publishEventsWithContext(
            InventoryTransaction transaction, TriggerContext triggerContext) {
        TriggerContext context = triggerContext != null ? triggerContext : TriggerContext.manual();

        transaction.getDomainEvents().stream()
                .map(event -> enrichWithTriggerContext(event, context))
                .forEach(eventPublisher::publishEvent);
        transaction.clearDomainEvents();
    }

    private Object enrichWithTriggerContext(Object event, TriggerContext triggerContext) {
        TriggerContext newContext =
                TriggerContext.of(
                        "OrderReadyForFulfillmentEvent",
                        triggerContext.getCorrelationId(),
                        triggerContext.getTriggerBy());

        if (event instanceof InventoryReservedEvent original) {
            return new InventoryReservedEvent(
                    original.getTransactionId(),
                    original.getOrderId(),
                    original.getExternalReservationId(),
                    original.getOccurredAt(),
                    newContext);
        } else if (event instanceof ReservationFailedEvent original) {
            return new ReservationFailedEvent(
                    original.getTransactionId(),
                    original.getOrderId(),
                    original.getReason(),
                    original.getOccurredAt(),
                    newContext);
        } else if (event instanceof InventoryTransactionFailedEvent original) {
            return new InventoryTransactionFailedEvent(
                    original.getTransactionId(),
                    original.getType(),
                    original.getSource(),
                    original.getReason(),
                    original.getOccurredAt(),
                    newContext);
        }
        return event;
    }
}
