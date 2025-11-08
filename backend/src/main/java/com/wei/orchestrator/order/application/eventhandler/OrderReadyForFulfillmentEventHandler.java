package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.command.ReserveInventoryCommand;
import com.wei.orchestrator.order.domain.event.OrderReadyForFulfillmentEvent;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @EventListener
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
                            orderId, item.getSku(), "DEFAULT_WAREHOUSE", item.getQuantity());
            inventoryApplicationService.reserveInventory(command);
        }

        logger.info("Successfully initiated inventory reservation for order: {}", orderId);
    }
}
