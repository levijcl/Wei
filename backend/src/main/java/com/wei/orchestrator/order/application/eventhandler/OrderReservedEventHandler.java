package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.order.domain.event.OrderReservedEvent;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.wes.application.PickingTaskApplicationService;
import com.wei.orchestrator.wes.application.command.CreatePickingTaskForOrderCommand;
import com.wei.orchestrator.wes.application.command.dto.TaskItemDto;
import com.wei.orchestrator.wes.application.dto.WesOperationResultDto;
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
public class OrderReservedEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(OrderReservedEventHandler.class);

    private final OrderRepository orderRepository;
    private final PickingTaskApplicationService pickingTaskApplicationService;

    public OrderReservedEventHandler(
            OrderRepository orderRepository,
            PickingTaskApplicationService pickingTaskApplicationService) {
        this.orderRepository = orderRepository;
        this.pickingTaskApplicationService = pickingTaskApplicationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderReserved(OrderReservedEvent event) {
        String orderId = event.getOrderId();

        logger.info(
                "Handling OrderReservedEvent for order: {} at {}", orderId, event.getOccurredAt());

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new IllegalStateException("Order not found: " + orderId));

        List<TaskItemDto> taskItems =
                order.getOrderLineItems().stream()
                        .filter(
                                item ->
                                        item.getReservationInfo() != null
                                                && item.getReservationInfo().isReserved())
                        .map(
                                item ->
                                        new TaskItemDto(
                                                item.getSku(),
                                                item.getQuantity(),
                                                item.getReservationInfo().getWarehouseId()))
                        .collect(Collectors.toList());

        CreatePickingTaskForOrderCommand command =
                new CreatePickingTaskForOrderCommand(orderId, taskItems, 5);

        WesOperationResultDto result =
                pickingTaskApplicationService.createPickingTaskForOrder(command);

        if (!result.isSuccess()) {
            logger.error(
                    "Failed to create picking task for order: {}, error: {}",
                    orderId,
                    result.getErrorMessage());
        } else {
            logger.info(
                    "Successfully created picking task for order: {}, taskId: {}",
                    orderId,
                    result.getTaskId());
        }
    }
}
