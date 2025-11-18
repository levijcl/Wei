package com.wei.orchestrator.integration.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.wei.orchestrator.inventory.domain.event.InventoryReservedEvent;
import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.ExternalReservationId;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import com.wei.orchestrator.order.application.eventhandler.InventoryReservedEventHandler;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.port.WesPort;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class InventoryReservedEventHandlerIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;

    @Autowired private OrderRepository orderRepository;

    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired private InventoryReservedEventHandler eventHandler;

    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoBean private WesPort wesPort;

    @Nested
    class EventPublicationAndHandling {

        @Test
        void shouldHandleInventoryReservedEvent() {
            when(wesPort.submitPickingTask(any()))
                    .thenReturn(WesTaskId.of(UUID.randomUUID().toString()));
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String externalReservationId =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, "SKU-001", "WH-001", 10);
            transaction.markAsReserved(ExternalReservationId.of(externalReservationId));
            inventoryTransactionRepository.save(transaction);

            InventoryReservedEvent event =
                    new InventoryReservedEvent(
                            transaction.getTransactionId(),
                            orderId,
                            externalReservationId,
                            LocalDateTime.now());

            eventHandler.handleInventoryReserved(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.RESERVED, foundOrder.get().getStatus());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).isReserved());
            assertEquals(
                    "WH-001",
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getReservationInfo()
                            .getWarehouseId());
        }

        @Test
        void shouldHandlePartialReservationForMultipleLineItems() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String externalReservationId =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-100", 5, new BigDecimal("50.00")),
                                    new OrderLineItem("SKU-101", 3, new BigDecimal("30.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, "SKU-100", "WH-002", 5);
            transaction.markAsReserved(ExternalReservationId.of(externalReservationId));
            inventoryTransactionRepository.save(transaction);

            InventoryReservedEvent event =
                    new InventoryReservedEvent(
                            transaction.getTransactionId(),
                            orderId,
                            externalReservationId,
                            LocalDateTime.now());

            eventHandler.handleInventoryReserved(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.PARTIALLY_RESERVED, foundOrder.get().getStatus());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).isReserved());
            assertFalse(foundOrder.get().getOrderLineItems().get(1).isReserved());
        }

        @Test
        void shouldDirectlyInvokeHandlerMethod() {
            when(wesPort.submitPickingTask(any()))
                    .thenReturn(WesTaskId.of(UUID.randomUUID().toString()));
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String externalReservationId =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-200", 7, new BigDecimal("70.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, "SKU-200", "WH-003", 7);
            transaction.markAsReserved(ExternalReservationId.of(externalReservationId));
            inventoryTransactionRepository.save(transaction);

            InventoryReservedEvent event =
                    new InventoryReservedEvent(
                            transaction.getTransactionId(),
                            orderId,
                            externalReservationId,
                            LocalDateTime.now());

            eventHandler.handleInventoryReserved(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.RESERVED, foundOrder.get().getStatus());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).isReserved());
        }

        @Test
        void shouldSkipProcessingWhenOrderNotFound() {
            String orderId = "NON-EXISTENT-ORDER";
            String transactionId = UUID.randomUUID().toString();
            String externalReservationId =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);

            InventoryReservedEvent event =
                    new InventoryReservedEvent(
                            transactionId, orderId, externalReservationId, LocalDateTime.now());

            eventHandler.handleInventoryReserved(event);
        }

        @Test
        void shouldThrowExceptionWhenTransactionNotFound() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String transactionId = "NON-EXISTENT-TX";
            String externalReservationId =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-300", 3, new BigDecimal("30.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            InventoryReservedEvent event =
                    new InventoryReservedEvent(
                            transactionId, orderId, externalReservationId, LocalDateTime.now());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleInventoryReserved(event);
                            });

            assertTrue(exception.getMessage().contains("InventoryTransaction not found"));
        }

        @Test
        void shouldStoreReservationInfoCorrectly() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String externalReservationId =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-400", 2, new BigDecimal("20.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, "SKU-400", "WH-004", 2);
            transaction.markAsReserved(ExternalReservationId.of(externalReservationId));
            inventoryTransactionRepository.save(transaction);

            InventoryReservedEvent event =
                    new InventoryReservedEvent(
                            transaction.getTransactionId(),
                            orderId,
                            externalReservationId,
                            LocalDateTime.now());

            eventHandler.handleInventoryReserved(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());

            OrderLineItem lineItem = foundOrder.get().getOrderLineItems().get(0);
            assertTrue(lineItem.isReserved());
            assertEquals("WH-004", lineItem.getReservationInfo().getWarehouseId());
            assertEquals(
                    externalReservationId,
                    lineItem.getReservationInfo().getExternalReservationId());
            assertEquals(
                    transaction.getTransactionId(),
                    lineItem.getReservationInfo().getTransactionId());
        }
    }

    @Nested
    class TransactionIsolation {

        @Test
        void shouldCommitOrderUpdateWhenHandlerSucceeds() {
            when(wesPort.submitPickingTask(any()))
                    .thenReturn(WesTaskId.of(UUID.randomUUID().toString()));
            String orderId = "SUCCESS-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String externalReservationId =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        Order order =
                                new Order(
                                        orderId,
                                        List.of(
                                                new OrderLineItem(
                                                        "SKU-500", 1, new BigDecimal("10.00"))));
                        order.markReadyForFulfillment();
                        orderRepository.save(order);
                        return null;
                    });

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, "SKU-500", "WH-005", 1);
            transaction.markAsReserved(ExternalReservationId.of(externalReservationId));
            inventoryTransactionRepository.save(transaction);

            InventoryReservedEvent event =
                    new InventoryReservedEvent(
                            transaction.getTransactionId(),
                            orderId,
                            externalReservationId,
                            LocalDateTime.now());

            eventHandler.handleInventoryReserved(event);

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent());
            assertEquals(OrderStatus.RESERVED, order.get().getStatus());
            assertTrue(order.get().getOrderLineItems().get(0).isReserved());
        }

        @Test
        void shouldRollbackWhenHandlerFails() {
            String orderId = "FAIL-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String externalReservationId =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        Order order =
                                new Order(
                                        orderId,
                                        List.of(
                                                new OrderLineItem(
                                                        "SKU-600", 2, new BigDecimal("20.00"))));
                        order.markReadyForFulfillment();
                        orderRepository.save(order);
                        return null;
                    });

            String nonExistentTransactionId = "NON-EXISTENT-TX";
            InventoryReservedEvent event =
                    new InventoryReservedEvent(
                            nonExistentTransactionId,
                            orderId,
                            externalReservationId,
                            LocalDateTime.now());

            assertThrows(
                    IllegalStateException.class,
                    () -> {
                        eventHandler.handleInventoryReserved(event);
                    });

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent());
            assertEquals(
                    OrderStatus.AWAITING_FULFILLMENT,
                    order.get().getStatus(),
                    "Order status should remain AWAITING_FULFILLMENT");
            assertFalse(
                    order.get().getOrderLineItems().get(0).isReserved(),
                    "Line item should not be reserved");
        }
    }

    @Nested
    class MultipleLineItemScenarios {

        @Test
        void shouldHandleReservationForAllLineItemsSequentially() {
            when(wesPort.submitPickingTask(any()))
                    .thenReturn(WesTaskId.of(UUID.randomUUID().toString()));
            String orderId = "MULTI-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-700", 1, new BigDecimal("10.00")),
                                    new OrderLineItem("SKU-701", 2, new BigDecimal("20.00")),
                                    new OrderLineItem("SKU-702", 3, new BigDecimal("30.00"))));
            order.markReadyForFulfillment();
            orderRepository.save(order);

            String externalReservationId1 =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);
            InventoryTransaction transaction1 =
                    InventoryTransaction.createReservation(orderId, "SKU-700", "WH-007", 1);
            transaction1.markAsReserved(ExternalReservationId.of(externalReservationId1));
            inventoryTransactionRepository.save(transaction1);

            InventoryReservedEvent event1 =
                    new InventoryReservedEvent(
                            transaction1.getTransactionId(),
                            orderId,
                            externalReservationId1,
                            LocalDateTime.now());
            eventHandler.handleInventoryReserved(event1);

            Optional<Order> partialOrder = orderRepository.findById(orderId);
            assertTrue(partialOrder.isPresent());
            assertEquals(OrderStatus.PARTIALLY_RESERVED, partialOrder.get().getStatus());

            String externalReservationId2 =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);
            InventoryTransaction transaction2 =
                    InventoryTransaction.createReservation(orderId, "SKU-701", "WH-007", 2);
            transaction2.markAsReserved(ExternalReservationId.of(externalReservationId2));
            inventoryTransactionRepository.save(transaction2);

            InventoryReservedEvent event2 =
                    new InventoryReservedEvent(
                            transaction2.getTransactionId(),
                            orderId,
                            externalReservationId2,
                            LocalDateTime.now());
            eventHandler.handleInventoryReserved(event2);

            Optional<Order> stillPartialOrder = orderRepository.findById(orderId);
            assertTrue(stillPartialOrder.isPresent());
            assertEquals(OrderStatus.PARTIALLY_RESERVED, stillPartialOrder.get().getStatus());

            String externalReservationId3 =
                    "EXT-RES-" + UUID.randomUUID().toString().substring(0, 8);
            InventoryTransaction transaction3 =
                    InventoryTransaction.createReservation(orderId, "SKU-702", "WH-007", 3);
            transaction3.markAsReserved(ExternalReservationId.of(externalReservationId3));
            inventoryTransactionRepository.save(transaction3);

            InventoryReservedEvent event3 =
                    new InventoryReservedEvent(
                            transaction3.getTransactionId(),
                            orderId,
                            externalReservationId3,
                            LocalDateTime.now());
            eventHandler.handleInventoryReserved(event3);

            Optional<Order> fullyReservedOrder = orderRepository.findById(orderId);
            assertTrue(fullyReservedOrder.isPresent());
            assertEquals(OrderStatus.RESERVED, fullyReservedOrder.get().getStatus());
            assertTrue(fullyReservedOrder.get().isFullyReserved());
        }
    }
}
