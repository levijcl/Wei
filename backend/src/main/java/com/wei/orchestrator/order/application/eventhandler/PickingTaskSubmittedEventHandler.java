package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.wes.domain.event.PickingTaskSubmittedEvent;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
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

@Component
public class PickingTaskSubmittedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(PickingTaskSubmittedEventHandler.class);

    private final OrderRepository orderRepository;
    private final PickingTaskRepository pickingTaskRepository;

    public PickingTaskSubmittedEventHandler(
            OrderRepository orderRepository, PickingTaskRepository pickingTaskRepository) {
        this.orderRepository = orderRepository;
        this.pickingTaskRepository = pickingTaskRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePickingTaskSubmitted(PickingTaskSubmittedEvent event) {
        if (event.getOrigin() != TaskOrigin.ORCHESTRATOR_SUBMITTED) {
            logger.info(
                    "Skipping PickingTaskSubmittedEvent for task {} with origin {}",
                    event.getTaskId(),
                    event.getOrigin());
            return;
        }

        String taskId = event.getTaskId();
        logger.info("Handling PickingTaskSubmittedEvent for task: {}", taskId);

        PickingTask pickingTask =
                pickingTaskRepository
                        .findById(taskId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Picking task not found: " + taskId));

        String orderId = pickingTask.getOrderId();
        if (orderId == null) {
            logger.warn("Picking task {} has no orderId, skipping", taskId);
            return;
        }

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new IllegalStateException("Order not found: " + orderId));

        List<String> skus =
                pickingTask.getItems().stream()
                        .map(TaskItem::getSku)
                        .collect(Collectors.toList());

        order.markItemsAsPickingInProgress(skus, taskId);

        orderRepository.save(order);

        logger.info(
                "Successfully marked line items as picking in progress for order: {}, task: {}",
                orderId,
                taskId);
    }
}
