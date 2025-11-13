package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.wes.domain.event.PickingTaskCanceledEvent;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("OrderPickingTaskCanceledEventHandler")
public class PickingTaskCanceledEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(PickingTaskCanceledEventHandler.class);

    private final OrderApplicationService orderApplicationService;
    private final PickingTaskRepository pickingTaskRepository;

    public PickingTaskCanceledEventHandler(
            OrderApplicationService orderApplicationService,
            PickingTaskRepository pickingTaskRepository) {
        this.orderApplicationService = orderApplicationService;
        this.pickingTaskRepository = pickingTaskRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePickingTaskCanceled(PickingTaskCanceledEvent event) {
        String orderId = event.getOrderId();
        if (orderId == null) {
            logger.warn("PickingTaskCanceledEvent has no orderId, skipping");
            return;
        }

        String taskId = event.getTaskId();
        String reason = event.getReason();
        logger.info(
                "Handling PickingTaskCanceledEvent for task: {}, order: {}, reason: {}",
                taskId,
                orderId,
                reason);

        PickingTask pickingTask =
                pickingTaskRepository
                        .findById(taskId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Picking task not found: " + taskId));

        List<String> skus =
                pickingTask.getItems().stream().map(TaskItem::getSku).collect(Collectors.toList());

        orderApplicationService.markOrderItemsAsPickingCanceled(orderId, skus, reason);

        logger.info(
                "Successfully marked line items as picking canceled for order: {}, task: {}",
                orderId,
                taskId);
    }
}
