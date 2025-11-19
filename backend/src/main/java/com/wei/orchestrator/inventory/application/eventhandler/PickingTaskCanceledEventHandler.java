package com.wei.orchestrator.inventory.application.eventhandler;

import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.wes.domain.event.PickingTaskCanceledEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("InventoryPickingTaskCanceledEventHandler")
public class PickingTaskCanceledEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(PickingTaskCanceledEventHandler.class);

    private final InventoryApplicationService inventoryApplicationService;

    public PickingTaskCanceledEventHandler(
            InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePickingTaskCanceled(PickingTaskCanceledEvent event) {
        logger.info("Handling picking task canceled event for task: {}", event.getTaskId());

        if (event.getOrderId() == null || event.getOrderId().isBlank()) {
            logger.warn(
                    "Picking task {} has no order ID, skipping inventory release",
                    event.getTaskId());
            return;
        }

        List<InventoryOperationResultDto> resultList =
                inventoryApplicationService.releaseReservationForOrder(
                        event.getOrderId(), event.getReason());

        for (InventoryOperationResultDto result : resultList) {

            if (result.isSuccess()) {
                logger.info(
                        "Successfully released reservation for order: {} after picking task: {} was"
                                + " canceled",
                        event.getOrderId(),
                        event.getTaskId());
            } else {
                logger.error(
                        "Failed to release reservation for order: {} after picking task: {} was"
                                + " canceled, error: {}",
                        event.getOrderId(),
                        event.getTaskId(),
                        result.getErrorMessage());
            }
        }
    }
}
