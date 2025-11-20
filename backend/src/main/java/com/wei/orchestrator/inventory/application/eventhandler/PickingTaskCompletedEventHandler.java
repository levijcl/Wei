package com.wei.orchestrator.inventory.application.eventhandler;

import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.wes.domain.event.PickingTaskCompletedEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("InventoryPickingTaskCompletedEventHandler")
public class PickingTaskCompletedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(PickingTaskCompletedEventHandler.class);

    private final InventoryApplicationService inventoryApplicationService;

    public PickingTaskCompletedEventHandler(
            InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePickingTaskCompleted(PickingTaskCompletedEvent event) {
        logger.info("Handling picking task completed event for task: {}", event.getTaskId());

        if (event.getOrderId() == null || event.getOrderId().isBlank()) {
            logger.warn(
                    "Picking task {} has no order ID, skipping inventory consumption",
                    event.getTaskId());
            return;
        }

        TriggerContext triggerContext = event.getTriggerContext();

        List<InventoryOperationResultDto> results =
                inventoryApplicationService.consumeReservationForOrder(
                        event.getOrderId(), triggerContext);
        for (InventoryOperationResultDto result : results) {
            if (result.isSuccess()) {
                logger.info(
                        "Successfully consumed reservation for order: {} after picking task: {}",
                        event.getOrderId(),
                        event.getTaskId());
            } else {
                logger.error(
                        "Failed to consume reservation for order: {} after picking task: {}, error:"
                                + " {}",
                        event.getOrderId(),
                        event.getTaskId(),
                        result.getErrorMessage());
            }
        }
    }
}
