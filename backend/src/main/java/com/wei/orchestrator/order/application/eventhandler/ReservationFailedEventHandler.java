package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.inventory.domain.event.ReservationFailedEvent;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionLine;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReservationFailedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ReservationFailedEventHandler.class);

    private final OrderRepository orderRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public ReservationFailedEventHandler(
            OrderRepository orderRepository,
            InventoryTransactionRepository inventoryTransactionRepository) {
        this.orderRepository = orderRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReservationFailed(ReservationFailedEvent failedEvent) {
        String orderId = failedEvent.getOrderId();
        String transactionId = failedEvent.getTransactionId();
        String reason = failedEvent.getReason();

        logger.warn(
                "Handling ReservationFailedEvent for order: {}, transactionId: {}, reason: {}",
                orderId,
                transactionId,
                reason);

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            logger.warn(
                    "Order not found for ReservationFailedEvent: {}. Skipping event processing.",
                    orderId);
            return;
        }
        Order order = orderOpt.get();

        InventoryTransaction transaction =
                inventoryTransactionRepository
                        .findById(transactionId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "InventoryTransaction not found: "
                                                        + transactionId));

        List<TransactionLine> transactionLines = transaction.getTransactionLines();
        if (transactionLines == null || transactionLines.isEmpty()) {
            throw new IllegalStateException("InventoryTransaction has no lines: " + transactionId);
        }

        TransactionLine transactionLine = transactionLines.get(0);
        String sku = transactionLine.getSku();

        OrderLineItem matchingLineItem =
                order.getOrderLineItems().stream()
                        .filter(item -> item.getSku().equals(sku))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Order line item not found for SKU: " + sku));

        order.markLineReservationFailed(matchingLineItem.getLineItemId(), reason);

        orderRepository.save(order);

        logger.warn(
                "Marked order {} line item as reservation failed for SKU: {}, lineItemId: {}, order"
                        + " status: {}",
                orderId,
                sku,
                matchingLineItem.getLineItemId(),
                order.getStatus());

        if (order.hasAnyReservationFailed() && !order.isPartiallyReserved()) {
            order.markAsFailedToReserve(
                    "All line items failed to reserve. First reason: " + reason);
            orderRepository.save(order);
            logger.error("Order {} marked as FAILED_TO_RESERVE - all items failed", orderId);
        }
    }
}
