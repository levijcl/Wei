package com.wei.orchestrator.integration.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.observation.domain.event.NewOrderObservedEvent;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import com.wei.orchestrator.order.application.eventhandler.NewOrderObservedEventHandler;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NewOrderObservedEventHandlerIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;

    @Autowired private OrderRepository orderRepository;

    @Autowired private NewOrderObservedEventHandler eventHandler;

    @Nested
    class EventPublicationAndHandling {

        @Test
        void shouldCreateOrderWhenNewOrderObservedEventIsPublished() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            NewOrderObservedEvent event = createTestEvent(orderId);

            eventPublisher.publishEvent(event);

            Optional<Order> createdOrder = orderRepository.findById(orderId);
            assertTrue(createdOrder.isPresent(), "Order should be created in database");
            assertEquals(orderId, createdOrder.get().getOrderId());
            assertEquals(OrderStatus.CREATED, createdOrder.get().getStatus());
            assertEquals(2, createdOrder.get().getOrderLineItems().size());
        }

        @Test
        void shouldPersistOrderLineItemsCorrectly() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            NewOrderObservedEvent event = createTestEvent(orderId);

            eventPublisher.publishEvent(event);

            Optional<Order> createdOrder = orderRepository.findById(orderId);
            assertTrue(createdOrder.isPresent());

            Order order = createdOrder.get();
            assertEquals("SKU-001", order.getOrderLineItems().get(0).getSku());
            assertEquals(10, order.getOrderLineItems().get(0).getQuantity());
            assertEquals(new BigDecimal("100.00"), order.getOrderLineItems().get(0).getPrice());

            assertEquals("SKU-002", order.getOrderLineItems().get(1).getSku());
            assertEquals(5, order.getOrderLineItems().get(1).getQuantity());
            assertEquals(new BigDecimal("50.00"), order.getOrderLineItems().get(1).getPrice());
        }

        @Test
        void shouldHandleIdempotencyWhenSameEventPublishedMultipleTimes() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            NewOrderObservedEvent event = createTestEvent(orderId);

            eventPublisher.publishEvent(event);

            Optional<Order> firstOrder = orderRepository.findById(orderId);
            assertTrue(firstOrder.isPresent(), "Order should be created on first event");

            eventPublisher.publishEvent(event);

            Optional<Order> secondOrder = orderRepository.findById(orderId);
            assertTrue(secondOrder.isPresent(), "Order should still exist");
            assertEquals(
                    firstOrder.get().getOrderId(),
                    secondOrder.get().getOrderId(),
                    "Should be the same order");
        }

        @Test
        void shouldHandleMultipleOrdersFromDifferentEvents() {
            String orderId1 = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId2 = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            NewOrderObservedEvent event1 = createTestEvent(orderId1);
            NewOrderObservedEvent event2 = createTestEvent(orderId2);

            eventPublisher.publishEvent(event1);
            eventPublisher.publishEvent(event2);

            Optional<Order> order1 = orderRepository.findById(orderId1);
            Optional<Order> order2 = orderRepository.findById(orderId2);

            assertTrue(order1.isPresent(), "First order should be created");
            assertTrue(order2.isPresent(), "Second order should be created");
            assertNotEquals(
                    order1.get().getOrderId(),
                    order2.get().getOrderId(),
                    "Orders should have different IDs");
        }

        @Test
        void shouldHandleEventWithMultipleLineItems() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<ObservedOrderItem> items =
                    Arrays.asList(
                            new ObservedOrderItem(
                                    "SKU-100", "Product 100", 5, new BigDecimal("50.00")),
                            new ObservedOrderItem(
                                    "SKU-101", "Product 101", 10, new BigDecimal("100.00")),
                            new ObservedOrderItem(
                                    "SKU-102", "Product 102", 15, new BigDecimal("150.00")));

            ObservationResult observationResult =
                    new ObservationResult(
                            orderId,
                            "Jane Doe",
                            "jane@example.com",
                            "456 Oak St",
                            "EXPRESS",
                            "WH-002",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());

            NewOrderObservedEvent event =
                    new NewOrderObservedEvent("observer-1", observationResult);

            eventPublisher.publishEvent(event);

            Optional<Order> createdOrder = orderRepository.findById(orderId);
            assertTrue(createdOrder.isPresent());
            assertEquals(3, createdOrder.get().getOrderLineItems().size());

            assertEquals("SKU-100", createdOrder.get().getOrderLineItems().get(0).getSku());
            assertEquals("SKU-101", createdOrder.get().getOrderLineItems().get(1).getSku());
            assertEquals("SKU-102", createdOrder.get().getOrderLineItems().get(2).getSku());
        }

        @Test
        void shouldHandleEventWithDecimalPrices() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<ObservedOrderItem> items =
                    Arrays.asList(
                            new ObservedOrderItem(
                                    "SKU-200", "Product 200", 1, new BigDecimal("99.99")),
                            new ObservedOrderItem(
                                    "SKU-201", "Product 201", 2, new BigDecimal("0.01")));

            ObservationResult observationResult =
                    new ObservationResult(
                            orderId,
                            "Bob Smith",
                            "bob@example.com",
                            "789 Pine St",
                            "STANDARD",
                            "WH-001",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());

            NewOrderObservedEvent event =
                    new NewOrderObservedEvent("observer-2", observationResult);

            eventPublisher.publishEvent(event);

            Optional<Order> createdOrder = orderRepository.findById(orderId);
            assertTrue(createdOrder.isPresent());

            assertEquals(
                    new BigDecimal("99.99"),
                    createdOrder.get().getOrderLineItems().get(0).getPrice());
            assertEquals(
                    new BigDecimal("0.01"),
                    createdOrder.get().getOrderLineItems().get(1).getPrice());
        }

        @Test
        void shouldDirectlyInvokeHandlerMethodAndCreateOrder() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            NewOrderObservedEvent event = createTestEvent(orderId);

            eventHandler.handleNewOrderObserved(event);

            Optional<Order> createdOrder = orderRepository.findById(orderId);
            assertTrue(createdOrder.isPresent());
            assertEquals(orderId, createdOrder.get().getOrderId());
            assertEquals(OrderStatus.CREATED, createdOrder.get().getStatus());
        }

        @Test
        void shouldSkipOrderCreationWhenOrderAlreadyExistsFromDirectInvocation() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            NewOrderObservedEvent event = createTestEvent(orderId);

            eventHandler.handleNewOrderObserved(event);

            Optional<Order> firstOrder = orderRepository.findById(orderId);
            assertTrue(firstOrder.isPresent());

            eventHandler.handleNewOrderObserved(event);

            Optional<Order> secondOrder = orderRepository.findById(orderId);
            assertTrue(secondOrder.isPresent());
            assertEquals(firstOrder.get().getOrderId(), secondOrder.get().getOrderId());
        }
    }

    private NewOrderObservedEvent createTestEvent(String orderId) {
        List<ObservedOrderItem> items =
                Arrays.asList(
                        new ObservedOrderItem("SKU-001", "Product 1", 10, new BigDecimal("100.00")),
                        new ObservedOrderItem("SKU-002", "Product 2", 5, new BigDecimal("50.00")));

        ObservationResult observationResult =
                new ObservationResult(
                        orderId,
                        "John Doe",
                        "john@example.com",
                        "123 Main St",
                        "STANDARD",
                        "WH-001",
                        "NEW",
                        null,
                        items,
                        LocalDateTime.now());

        return new NewOrderObservedEvent("observer-1", observationResult);
    }
}
