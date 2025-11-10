package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.inventory.domain.event.InventoryReservedEvent;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InventoryReservedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(InventoryReservedEventHandler.class);

    private final OrderRepository orderRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryReservedEventHandler(
            OrderRepository orderRepository,
            InventoryTransactionRepository inventoryTransactionRepository) {
        this.orderRepository = orderRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleInventoryReserved(InventoryReservedEvent reservedEvent) {
        String orderId = reservedEvent.getOrderId();
        String transactionId = reservedEvent.getTransactionId();
        String externalReservationId = reservedEvent.getExternalReservationId();

        logger.info(
                "Handling InventoryReservedEvent for order: {}, transactionId: {},"
                        + " externalReservationId: {}",
                orderId,
                transactionId,
                externalReservationId);

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            logger.warn(
                    "Order not found for InventoryReservedEvent: {}. Skipping event processing.",
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

        String warehouseId = transaction.getWarehouseLocation().getWarehouseId();

        for (TransactionLine transactionLine : transactionLines) {
            String sku = transactionLine.getSku();

            OrderLineItem matchingLineItem =
                    order.getOrderLineItems().stream()
                            .filter(item -> item.getSku().equals(sku))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Order line item not found for SKU: "
                                                            + sku
                                                            + " in transaction: "
                                                            + transactionId));

            order.reserveLineItem(
                    matchingLineItem.getLineItemId(),
                    transactionId,
                    externalReservationId,
                    warehouseId);

            logger.info(
                    "Reserved line item for order {}, SKU: {}, lineItemId: {}",
                    orderId,
                    sku,
                    matchingLineItem.getLineItemId());
        }

        orderRepository.save(order);

        logger.info(
                "Successfully updated order {} with reservations for {} line item(s), new order"
                        + " status: {}",
                orderId,
                transactionLines.size(),
                order.getStatus());
    }
}
