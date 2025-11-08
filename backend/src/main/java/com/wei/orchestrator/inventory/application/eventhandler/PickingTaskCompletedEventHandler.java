package com.wei.orchestrator.inventory.application.eventhandler;

import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.command.ConsumeReservationCommand;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import com.wei.orchestrator.wes.domain.event.PickingTaskCompletedEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PickingTaskCompletedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(PickingTaskCompletedEventHandler.class);

    private final InventoryApplicationService inventoryApplicationService;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public PickingTaskCompletedEventHandler(
            InventoryApplicationService inventoryApplicationService,
            InventoryTransactionRepository inventoryTransactionRepository) {
        this.inventoryApplicationService = inventoryApplicationService;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePickingTaskCompleted(PickingTaskCompletedEvent event) {
        logger.info("Handling picking task completed event for task: {}", event.getTaskId());

        if (event.getOrderId() == null || event.getOrderId().isBlank()) {
            logger.warn(
                    "Picking task {} has no order ID, skipping inventory consumption",
                    event.getTaskId());
            return;
        }

        List<InventoryTransaction> transactions =
                inventoryTransactionRepository.findBySourceReferenceId(event.getOrderId());

        InventoryTransaction reservationTransaction =
                transactions.stream()
                        .filter(t -> t.getExternalReservationId() != null)
                        .filter(t -> t.getStatus().canFail())
                        .findFirst()
                        .orElse(null);

        if (reservationTransaction == null) {
            logger.warn(
                    "No reservation transaction found for order: {}, skipping inventory"
                            + " consumption",
                    event.getOrderId());
            return;
        }

        ConsumeReservationCommand command =
                new ConsumeReservationCommand(
                        reservationTransaction.getTransactionId(),
                        reservationTransaction.getExternalReservationId().getValue(),
                        event.getOrderId());

        InventoryOperationResultDto result = inventoryApplicationService.consumeReservation(command);

        if (result.isSuccess()) {
            logger.info(
                    "Successfully consumed reservation for order: {} after picking task: {}",
                    event.getOrderId(),
                    event.getTaskId());
        } else {
            logger.error(
                    "Failed to consume reservation for order: {} after picking task: {}, error: {}",
                    event.getOrderId(),
                    event.getTaskId(),
                    result.getErrorMessage());
        }
    }
}
