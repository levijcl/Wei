package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.command.ReserveInventoryCommand;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.order.domain.event.OrderReadyForFulfillmentEvent;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderReadyForFulfillmentEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(OrderReadyForFulfillmentEventHandler.class);

    private final OrderRepository orderRepository;
    private final InventoryApplicationService inventoryApplicationService;

    public OrderReadyForFulfillmentEventHandler(
            OrderRepository orderRepository,
            InventoryApplicationService inventoryApplicationService) {
        this.orderRepository = orderRepository;
        this.inventoryApplicationService = inventoryApplicationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderReadyForFulfillment(OrderReadyForFulfillmentEvent event) {
        String orderId = event.getOrderId();

        logger.info(
                "Handling OrderReadyForFulfillmentEvent for order: {} at {}",
                orderId,
                event.getOccurredAt());

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new IllegalStateException("Order not found: " + orderId));

        List<OrderLineItem> lineItems = order.getOrderLineItems();

        for (OrderLineItem item : lineItems) {
            ReserveInventoryCommand command =
                    new ReserveInventoryCommand(
                            orderId, item.getSku(), "WH001", item.getQuantity());
            InventoryOperationResultDto result =
                    inventoryApplicationService.reserveInventory(command);

            if (!result.isSuccess()) {
                logger.error(
                        "Failed to reserve inventory for order: {}, SKU: {}, error: {}",
                        orderId,
                        item.getSku(),
                        result.getErrorMessage());
            } else {
                logger.info(
                        "Successfully reserved inventory for order: {}, SKU: {}, transactionId: {}",
                        orderId,
                        item.getSku(),
                        result.getTransactionId());
            }
        }

        logger.info("Completed inventory reservation process for order: {}", orderId);
    }
}
