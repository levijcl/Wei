package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.wes.domain.event.PickingTaskFailedEvent;
import com.wei.orchestrator.wes.domain.model.PickingTask;
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
public class PickingTaskFailedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(PickingTaskFailedEventHandler.class);

    private final OrderRepository orderRepository;
    private final PickingTaskRepository pickingTaskRepository;

    public PickingTaskFailedEventHandler(
            OrderRepository orderRepository, PickingTaskRepository pickingTaskRepository) {
        this.orderRepository = orderRepository;
        this.pickingTaskRepository = pickingTaskRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePickingTaskFailed(PickingTaskFailedEvent event) {
        if (event.getOrigin() != TaskOrigin.ORCHESTRATOR_SUBMITTED) {
            logger.info(
                    "Skipping PickingTaskFailedEvent for task {} with origin {}",
                    event.getTaskId(),
                    event.getOrigin());
            return;
        }

        String orderId = event.getOrderId();
        if (orderId == null) {
            logger.warn("PickingTaskFailedEvent has no orderId, skipping");
            return;
        }

        String taskId = event.getTaskId();
        logger.info(
                "Handling PickingTaskFailedEvent for task: {}, order: {}, reason: {}",
                taskId,
                orderId,
                event.getReason());

        PickingTask pickingTask =
                pickingTaskRepository
                        .findById(taskId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Picking task not found: " + taskId));

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new IllegalStateException("Order not found: " + orderId));

        List<String> skus =
                pickingTask.getItems().stream()
                        .map(item -> item.getSku())
                        .collect(Collectors.toList());

        order.markItemsAsPickingFailed(skus, event.getReason());

        orderRepository.save(order);

        logger.info(
                "Successfully marked line items as picking failed for order: {}, task: {}, new"
                        + " status: {}",
                orderId,
                taskId,
                order.getStatus());
    }
}
