package com.wei.orchestrator.integration.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.event.ReservationFailedEvent;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
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
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class ReservationFailedEventHandlerIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;

    @Autowired private OrderRepository orderRepository;

    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired private ReservationFailedEventHandler eventHandler;

    @Autowired private TransactionTemplate transactionTemplate;

    @Nested
    class EventPublicationAndHandling {

        @Test
        void shouldHandleReservationFailedEvent() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "Insufficient inventory";

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, "SKU-001", "WH-001", 10);
            transaction.fail(reason);
            inventoryTransactionRepository.save(transaction);

            ReservationFailedEvent event =
                    new ReservationFailedEvent(
                            transaction.getTransactionId(), orderId, reason, LocalDateTime.now());

            eventPublisher.publishEvent(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.FAILED_TO_RESERVE, foundOrder.get().getStatus());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).hasReservationFailed());
        }

        @Test
        void shouldHandlePartialFailureForMultipleLineItems() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "Out of stock";

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-100", 5, new BigDecimal("50.00")),
                                    new OrderLineItem("SKU-101", 3, new BigDecimal("30.00"))));
            order.markReadyForFulfillment();
            order.reserveLineItem(
                    order.getOrderLineItems().get(0).getLineItemId(),
                    "OTHER-TX",
                    "EXT-RES-001",
                    "WH-002");
            orderRepository.save(order);

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, "SKU-101", "WH-002", 3);
            transaction.fail(reason);
            inventoryTransactionRepository.save(transaction);

            ReservationFailedEvent event =
                    new ReservationFailedEvent(
                            transaction.getTransactionId(), orderId, reason, LocalDateTime.now());

            eventPublisher.publishEvent(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.PARTIALLY_RESERVED, foundOrder.get().getStatus());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).isReserved());
            assertTrue(foundOrder.get().getOrderLineItems().get(1).hasReservationFailed());
        }

        @Test
        void shouldDirectlyInvokeHandlerMethod() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "Warehouse unavailable";

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-200", 7, new BigDecimal("70.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, "SKU-200", "WH-003", 7);
            transaction.fail(reason);
            inventoryTransactionRepository.save(transaction);

            ReservationFailedEvent event =
                    new ReservationFailedEvent(
                            transaction.getTransactionId(), orderId, reason, LocalDateTime.now());

            eventHandler.handleReservationFailed(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.FAILED_TO_RESERVE, foundOrder.get().getStatus());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).hasReservationFailed());
        }

        @Test
        void shouldSkipProcessingWhenOrderNotFound() {
            String orderId = "NON-EXISTENT-ORDER";
            String transactionId = UUID.randomUUID().toString();
            String reason = "Test reason";

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            eventHandler.handleReservationFailed(event);
        }

        @Test
        void shouldThrowExceptionWhenTransactionNotFound() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String transactionId = "NON-EXISTENT-TX";
            String reason = "Test reason";

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-300", 3, new BigDecimal("30.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            ReservationFailedEvent event =
                    new ReservationFailedEvent(transactionId, orderId, reason, LocalDateTime.now());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleReservationFailed(event);
                            });

            assertTrue(exception.getMessage().contains("InventoryTransaction not found"));
        }

        @Test
        void shouldStoreFailureReasonCorrectly() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "Stock level too low";

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-400", 2, new BigDecimal("20.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, "SKU-400", "WH-004", 2);
            transaction.fail(reason);
            inventoryTransactionRepository.save(transaction);

            ReservationFailedEvent event =
                    new ReservationFailedEvent(
                            transaction.getTransactionId(), orderId, reason, LocalDateTime.now());

            eventHandler.handleReservationFailed(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());

            OrderLineItem lineItem = foundOrder.get().getOrderLineItems().get(0);
            assertTrue(lineItem.hasReservationFailed());
            assertEquals(reason, lineItem.getReservationInfo().getFailureReason());
        }

        @Test
        void shouldMarkAllLineItemsFailedForMultiLineTransaction() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "Warehouse offline";

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-500", 5, new BigDecimal("50.00")),
                                    new OrderLineItem("SKU-501", 3, new BigDecimal("30.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setTransactionId(UUID.randomUUID().toString());
            transaction.setType(
                    com.wei.orchestrator.inventory.domain.model.valueobject.TransactionType
                            .OUTBOUND);
            transaction.setStatus(
                    com.wei.orchestrator.inventory.domain.model.valueobject.TransactionStatus
                            .FAILED);
            transaction.setSource(
                    com.wei.orchestrator.inventory.domain.model.valueobject.TransactionSource
                            .ORDER_RESERVATION);
            transaction.setSourceReferenceId(orderId);
            transaction.setWarehouseLocation(
                    com.wei.orchestrator.inventory.domain.model.valueobject.WarehouseLocation.of(
                            "WH-005"));
            transaction.setFailureReason(reason);

            List<com.wei.orchestrator.inventory.domain.model.valueobject.TransactionLine>
                    transactionLines = new java.util.ArrayList<>();
            transactionLines.add(
                    com.wei.orchestrator.inventory.domain.model.valueobject.TransactionLine.of(
                            "SKU-500", 5));
            transactionLines.add(
                    com.wei.orchestrator.inventory.domain.model.valueobject.TransactionLine.of(
                            "SKU-501", 3));
            transaction.setTransactionLines(transactionLines);

            inventoryTransactionRepository.save(transaction);

            ReservationFailedEvent event =
                    new ReservationFailedEvent(
                            transaction.getTransactionId(), orderId, reason, LocalDateTime.now());

            eventHandler.handleReservationFailed(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.FAILED_TO_RESERVE, foundOrder.get().getStatus());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).hasReservationFailed());
            assertTrue(foundOrder.get().getOrderLineItems().get(1).hasReservationFailed());
            assertEquals(
                    reason,
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getReservationInfo()
                            .getFailureReason());
            assertEquals(
                    reason,
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(1)
                            .getReservationInfo()
                            .getFailureReason());
        }
    }

    @Nested
    class TransactionIsolation {

        @Test
        void shouldCommitOrderUpdateWhenHandlerSucceeds() {
            String orderId = "SUCCESS-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "Test failure";

            transactionTemplate.execute(
                    status -> {
                        Order order =
                                new Order(
                                        orderId,
                                        List.of(
                                                new OrderLineItem(
                                                        "SKU-600", 1, new BigDecimal("10.00"))));
                        order.markReadyForFulfillment();
                        orderRepository.save(order);
                        return null;
                    });

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, "SKU-600", "WH-006", 1);
            transaction.fail(reason);
            inventoryTransactionRepository.save(transaction);

            ReservationFailedEvent event =
                    new ReservationFailedEvent(
                            transaction.getTransactionId(), orderId, reason, LocalDateTime.now());

            eventHandler.handleReservationFailed(event);

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent());
            assertEquals(OrderStatus.FAILED_TO_RESERVE, order.get().getStatus());
            assertTrue(order.get().getOrderLineItems().get(0).hasReservationFailed());
        }

        @Test
        void shouldRollbackWhenHandlerFails() {
            String orderId = "FAIL-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "Test failure";

            transactionTemplate.execute(
                    status -> {
                        Order order =
                                new Order(
                                        orderId,
                                        List.of(
                                                new OrderLineItem(
                                                        "SKU-700", 2, new BigDecimal("20.00"))));
                        order.markReadyForFulfillment();
                        orderRepository.save(order);
                        return null;
                    });

            String nonExistentTx = "NON-EXISTENT-TX-" + UUID.randomUUID();
            ReservationFailedEvent event =
                    new ReservationFailedEvent(nonExistentTx, orderId, reason, LocalDateTime.now());

            assertThrows(
                    IllegalStateException.class,
                    () -> {
                        eventHandler.handleReservationFailed(event);
                    });

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent());
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, order.get().getStatus());
            assertFalse(order.get().getOrderLineItems().get(0).hasReservationFailed());
        }
    }

    @Nested
    class MultipleLineItemScenarios {

        @Test
        void shouldHandleAllLineItemsFailingSequentially() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason1 = "SKU-800 out of stock";
            String reason2 = "SKU-801 out of stock";

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-800", 4, new BigDecimal("40.00")),
                                    new OrderLineItem("SKU-801", 6, new BigDecimal("60.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            InventoryTransaction transaction1 =
                    InventoryTransaction.createReservation(orderId, "SKU-800", "WH-008", 4);
            transaction1.fail(reason1);
            inventoryTransactionRepository.save(transaction1);

            ReservationFailedEvent event1 =
                    new ReservationFailedEvent(
                            transaction1.getTransactionId(), orderId, reason1, LocalDateTime.now());

            eventHandler.handleReservationFailed(event1);

            Optional<Order> orderAfterFirst = orderRepository.findById(orderId);
            assertTrue(orderAfterFirst.isPresent());
            assertTrue(orderAfterFirst.get().getOrderLineItems().get(0).hasReservationFailed());
            assertFalse(orderAfterFirst.get().getOrderLineItems().get(1).hasReservationFailed());

            InventoryTransaction transaction2 =
                    InventoryTransaction.createReservation(orderId, "SKU-801", "WH-008", 6);
            transaction2.fail(reason2);
            inventoryTransactionRepository.save(transaction2);

            ReservationFailedEvent event2 =
                    new ReservationFailedEvent(
                            transaction2.getTransactionId(), orderId, reason2, LocalDateTime.now());

            eventHandler.handleReservationFailed(event2);

            Optional<Order> orderAfterSecond = orderRepository.findById(orderId);
            assertTrue(orderAfterSecond.isPresent());
            assertEquals(OrderStatus.FAILED_TO_RESERVE, orderAfterSecond.get().getStatus());
            assertTrue(orderAfterSecond.get().getOrderLineItems().get(0).hasReservationFailed());
            assertTrue(orderAfterSecond.get().getOrderLineItems().get(1).hasReservationFailed());
        }
    }
}
