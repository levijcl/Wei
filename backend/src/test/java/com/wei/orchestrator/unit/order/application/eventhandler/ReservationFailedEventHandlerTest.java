package com.wei.orchestrator.unit.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.inventory.domain.event.ReservationFailedEvent;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.WarehouseLocation;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import com.wei.orchestrator.order.application.eventhandler.ReservationFailedEventHandler;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationFailedEventHandlerTest {

    @Mock private OrderRepository orderRepository;

    @Mock private InventoryTransactionRepository inventoryTransactionRepository;

    @InjectMocks private ReservationFailedEventHandler eventHandler;

    @Nested
    class handleReservationFailedTest {

        @Test
        void shouldMarkOrderLineItemAsFailedWhenReservationFails() {
            String orderId = "ORDER-001";
            String transactionId = "TX-001";
            String reason = "Insufficient inventory";
            String warehouseId = "WH-001";

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            Order order = createMockOrder(orderId);
            OrderLineItem item = order.getOrderLineItems().get(0);
            InventoryTransaction transaction =
                    createTransactionWithSku(transactionId, orderId, warehouseId, item.getSku());

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryTransactionRepository.findById(transactionId))
                    .thenReturn(Optional.of(transaction));

            eventHandler.handleReservationFailed(event);

            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryTransactionRepository, times(1)).findById(transactionId);
            verify(orderRepository, times(2)).save(order);

            assertTrue(order.getOrderLineItems().get(0).hasReservationFailed());
            assertEquals(OrderStatus.FAILED_TO_RESERVE, order.getStatus());
        }

        @Test
        void shouldHandlePartialFailureForMultipleLineItems() {
            String orderId = "ORDER-002";
            String transactionId = "TX-002";
            String reason = "Insufficient inventory";
            String warehouseId = "WH-002";

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            Order order = createMultiItemOrder(orderId);
            order.reserveLineItem(
                    order.getOrderLineItems().get(0).getLineItemId(),
                    "OTHER-TX",
                    "EXT-RES-001",
                    warehouseId);

            OrderLineItem item = order.getOrderLineItems().get(1);
            InventoryTransaction transaction =
                    createTransactionWithSku(transactionId, orderId, warehouseId, item.getSku());

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryTransactionRepository.findById(transactionId))
                    .thenReturn(Optional.of(transaction));

            eventHandler.handleReservationFailed(event);

            verify(orderRepository, times(1)).save(order);

            assertTrue(order.getOrderLineItems().get(0).isReserved());
            assertTrue(order.getOrderLineItems().get(1).hasReservationFailed());
            assertEquals(OrderStatus.PARTIALLY_RESERVED, order.getStatus());
        }

        @Test
        void shouldSkipProcessingWhenOrderNotFound() {
            String orderId = "ORDER-003";
            String transactionId = "TX-003";
            String reason = "Insufficient inventory";

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            eventHandler.handleReservationFailed(event);

            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryTransactionRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenTransactionNotFound() {
            String orderId = "ORDER-004";
            String transactionId = "TX-004";
            String reason = "Insufficient inventory";

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            Order order = createMockOrder(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryTransactionRepository.findById(transactionId))
                    .thenReturn(Optional.empty());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleReservationFailed(event);
                            });

            assertTrue(exception.getMessage().contains("InventoryTransaction not found"));
            assertTrue(exception.getMessage().contains(transactionId));
            verify(orderRepository, times(1)).findById(orderId);
            verify(inventoryTransactionRepository, times(1)).findById(transactionId);
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenTransactionHasNoLines() {
            String orderId = "ORDER-005";
            String transactionId = "TX-005";
            String reason = "Insufficient inventory";
            String warehouseId = "WH-005";

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            Order order = createMockOrder(orderId);
            InventoryTransaction transaction =
                    createMockTransactionWithNoLines(transactionId, warehouseId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryTransactionRepository.findById(transactionId))
                    .thenReturn(Optional.of(transaction));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleReservationFailed(event);
                            });

            assertTrue(exception.getMessage().contains("InventoryTransaction has no lines"));
            assertTrue(exception.getMessage().contains(transactionId));
        }

        @Test
        void shouldThrowExceptionWhenLineItemNotFoundForSku() {
            String orderId = "ORDER-006";
            String transactionId = "TX-006";
            String reason = "Insufficient inventory";
            String warehouseId = "WH-006";

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            Order order = createMockOrder(orderId);
            InventoryTransaction transaction =
                    createTransactionWithSku(transactionId, orderId, warehouseId, "SKU-999");

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryTransactionRepository.findById(transactionId))
                    .thenReturn(Optional.of(transaction));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleReservationFailed(event);
                            });

            assertTrue(exception.getMessage().contains("Order line item not found for SKU"));
            assertTrue(exception.getMessage().contains("SKU-999"));
        }

        @Test
        void shouldMarkAllLineItemsAsFailedWhenAllFail() {
            String orderId = "ORDER-007";
            String transactionId = "TX-007";
            String reason = "Warehouse offline";
            String warehouseId = "WH-007";

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            Order order = createMultiItemOrder(orderId);
            InventoryTransaction transaction =
                    createTransactionWithMultipleSkus(
                            transactionId, orderId, warehouseId, "SKU-001", "SKU-002");

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryTransactionRepository.findById(transactionId))
                    .thenReturn(Optional.of(transaction));

            eventHandler.handleReservationFailed(event);

            verify(orderRepository, times(2)).save(order);

            assertTrue(order.getOrderLineItems().get(0).hasReservationFailed());
            assertTrue(order.getOrderLineItems().get(1).hasReservationFailed());
            assertEquals(OrderStatus.FAILED_TO_RESERVE, order.getStatus());
        }

        @Test
        void shouldHandleTransactionWithMultipleTransactionLines() {
            String orderId = "ORDER-008";
            String transactionId = "TX-008";
            String reason = "Insufficient inventory";
            String warehouseId = "WH-008";

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            Order order = createMultiItemOrder(orderId);
            InventoryTransaction transaction =
                    createTransactionWithMultipleSkus(
                            transactionId, orderId, warehouseId, "SKU-001", "SKU-002");

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryTransactionRepository.findById(transactionId))
                    .thenReturn(Optional.of(transaction));

            eventHandler.handleReservationFailed(event);

            verify(orderRepository, times(2)).save(order);

            assertTrue(order.getOrderLineItems().get(0).hasReservationFailed());
            assertTrue(order.getOrderLineItems().get(1).hasReservationFailed());
            assertEquals(OrderStatus.FAILED_TO_RESERVE, order.getStatus());
        }

        @Test
        void shouldThrowExceptionWhenTransactionLineSkuNotInOrder() {
            String orderId = "ORDER-009";
            String transactionId = "TX-009";
            String reason = "Insufficient inventory";
            String warehouseId = "WH-009";

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            Order order = createMultiItemOrder(orderId);
            InventoryTransaction transaction =
                    createTransactionWithMultipleSkus(
                            transactionId, orderId, warehouseId, "SKU-001", "SKU-999");

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(inventoryTransactionRepository.findById(transactionId))
                    .thenReturn(Optional.of(transaction));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleReservationFailed(event);
                            });

            assertTrue(exception.getMessage().contains("Order line item not found for SKU"));
            assertTrue(exception.getMessage().contains("SKU-999"));
            assertTrue(exception.getMessage().contains(transactionId));
        }
    }

    private Order createMockOrder(String orderId) {
        List<OrderLineItem> lineItems =
                List.of(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order(orderId, lineItems);
        order.markReadyForFulfillment();
        return order;
    }

    private Order createMultiItemOrder(String orderId) {
        List<OrderLineItem> lineItems =
                List.of(
                        new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")),
                        new OrderLineItem("SKU-002", 5, new BigDecimal("50.00")));
        Order order = new Order(orderId, lineItems);
        order.markReadyForFulfillment();
        return order;
    }

    private InventoryTransaction createTransactionWithSku(
            String transactionId, String orderId, String warehouseId, String sku) {
        InventoryTransaction transaction =
                InventoryTransaction.createReservation(orderId, sku, warehouseId, 1);
        transaction.setTransactionId(transactionId);

        return transaction;
    }

    private InventoryTransaction createMockTransactionWithNoLines(
            String transactionId, String warehouseId) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setWarehouseLocation(WarehouseLocation.of(warehouseId));

        return transaction;
    }

    private InventoryTransaction createTransactionWithMultipleSkus(
            String transactionId, String orderId, String warehouseId, String... skus) {
        if (skus.length == 0) {
            throw new IllegalArgumentException("At least one SKU must be provided");
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setType(
                com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType.OUTBOUND);
        transaction.setStatus(
                com.wei.orchestrator.inventory.domain.model.valueobject.TransactionStatus.PENDING);
        transaction.setSource(
                com.wei.orchestrator.inventory.domain.model.valueobject.TransactionSource
                        .ORDER_RESERVATION);
        transaction.setSourceReferenceId(orderId);
        transaction.setWarehouseLocation(
                com.wei.orchestrator.inventory.domain.model.valueobject.WarehouseLocation.of(
                        warehouseId));

        List<com.wei.orchestrator.inventory.domain.model.valueobject.TransactionLine>
                transactionLines = new java.util.ArrayList<>();
        for (String sku : skus) {
            transactionLines.add(
                    com.wei.orchestrator.inventory.domain.model.valueobject.TransactionLine.of(
                            sku, 1));
        }
        transaction.setTransactionLines(transactionLines);

        return transaction;
    }
}
