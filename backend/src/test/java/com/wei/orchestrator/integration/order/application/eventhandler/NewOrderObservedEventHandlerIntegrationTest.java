package com.wei.orchestrator.integration.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.wei.orchestrator.observation.domain.event.NewOrderObservedEvent;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.eventhandler.NewOrderObservedEventHandler;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.shared.domain.model.AuditRecord;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.shared.domain.repository.AuditRecordRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class NewOrderObservedEventHandlerIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;

    @Autowired private OrderRepository orderRepository;

    @Autowired private NewOrderObservedEventHandler eventHandler;

    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoSpyBean private OrderApplicationService orderApplicationService;

    @Autowired private AuditRecordRepository auditRecordRepository;

    @Nested
    class EventPublicationAndHandling {

        @Test
        void shouldCreateOrderWhenNewOrderObservedEventIsPublished() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            NewOrderObservedEvent event = createTestEvent(orderId);

            eventHandler.handleNewOrderObserved(event);

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

            eventHandler.handleNewOrderObserved(event);

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

            eventHandler.handleNewOrderObserved(event);

            Optional<Order> firstOrder = orderRepository.findById(orderId);
            assertTrue(firstOrder.isPresent(), "Order should be created on first event");

            eventHandler.handleNewOrderObserved(event);

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

            eventHandler.handleNewOrderObserved(event1);
            eventHandler.handleNewOrderObserved(event2);

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

            eventHandler.handleNewOrderObserved(event);

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

            eventHandler.handleNewOrderObserved(event);

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

    @Nested
    class TransactionIsolation {

        @Test
        void shouldNotRollbackCallerTransactionWhenHandlerFails() {
            String callerOrderId = "CALLER-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String handlerOrderId = "HANDLER-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        Order callerOrder =
                                new Order(
                                        callerOrderId,
                                        List.of(
                                                new OrderLineItem(
                                                        "SKU-CALLER", 5, new BigDecimal("50.00"))));
                        callerOrder.createOrder();
                        orderRepository.save(callerOrder);
                        return null;
                    });

            doThrow(new RuntimeException("Handler failed - simulating database error"))
                    .when(orderApplicationService)
                    .createOrder(any(), any());

            NewOrderObservedEvent event = createTestEvent(handlerOrderId);

            try {
                eventHandler.handleNewOrderObserved(event);
            } catch (Exception ignored) {
            }

            Optional<Order> savedCallerOrder = orderRepository.findById(callerOrderId);
            assertTrue(
                    savedCallerOrder.isPresent(),
                    "Caller's order should still be committed despite handler failure");
            assertEquals(OrderStatus.CREATED, savedCallerOrder.get().getStatus());
            assertEquals(1, savedCallerOrder.get().getOrderLineItems().size());

            Optional<Order> handlerOrder = orderRepository.findById(handlerOrderId);
            assertFalse(
                    handlerOrder.isPresent(), "Handler's order should not exist due to rollback");
        }

        @Test
        void shouldCommitBothTransactionsWhenHandlerSucceeds() {
            String callerOrderId = "CALLER-SUCCESS-" + UUID.randomUUID().toString().substring(0, 8);
            String handlerOrderId =
                    "HANDLER-SUCCESS-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        Order callerOrder =
                                new Order(
                                        callerOrderId,
                                        List.of(
                                                new OrderLineItem(
                                                        "SKU-CALLER", 3, new BigDecimal("30.00"))));
                        callerOrder.createOrder();
                        orderRepository.save(callerOrder);
                        return null;
                    });

            NewOrderObservedEvent event = createTestEvent(handlerOrderId);

            eventHandler.handleNewOrderObserved(event);

            Optional<Order> savedCallerOrder = orderRepository.findById(callerOrderId);
            assertTrue(savedCallerOrder.isPresent(), "Caller's order should be committed");
            assertEquals(OrderStatus.CREATED, savedCallerOrder.get().getStatus());

            Optional<Order> handlerOrder = orderRepository.findById(handlerOrderId);
            assertTrue(handlerOrder.isPresent(), "Handler's order should be committed");
            assertEquals(OrderStatus.CREATED, handlerOrder.get().getStatus());
        }
    }

    @Nested
    class EventCorrelation {

        @Test
        void shouldRecordBothEventsWithSameCorrelationIdWhenOrderScheduled() {
            String orderId = "CORR-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            UUID correlationId = UUID.randomUUID();

            TriggerContext triggerContext =
                    TriggerContext.of("Scheduled:OrderObserver", correlationId, null);

            LocalDateTime scheduledPickupTime = LocalDateTime.now().plusHours(3);

            NewOrderObservedEvent event =
                    createTestEventWithScheduling(orderId, scheduledPickupTime, triggerContext);

            transactionTemplate.execute(
                    status -> {
                        eventPublisher.publishEvent(event);
                        return null;
                    });

            Optional<Order> createdOrder = orderRepository.findById(orderId);
            assertTrue(createdOrder.isPresent(), "Order should be created");

            List<AuditRecord> auditRecords =
                    auditRecordRepository.findByCorrelationId(correlationId);

            assertFalse(auditRecords.isEmpty(), "Should have audit records");

            boolean allHaveSameCorrelationId =
                    auditRecords.stream()
                            .allMatch(
                                    record ->
                                            correlationId.equals(
                                                    record.getEventMetadata().getCorrelationId()));
            assertTrue(
                    allHaveSameCorrelationId,
                    "All audit records should share the same correlationId");

            List<String> eventNames = auditRecords.stream().map(AuditRecord::getEventName).toList();

            assertTrue(
                    eventNames.contains("NewOrderObservedEvent"),
                    "Should audit NewOrderObservedEvent");
            assertTrue(
                    eventNames.contains("OrderScheduledEvent"), "Should audit OrderScheduledEvent");

            AuditRecord newOrderRecord =
                    auditRecords.stream()
                            .filter(r -> "NewOrderObservedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "Scheduled:OrderObserver",
                    newOrderRecord.getEventMetadata().getTriggerSource(),
                    "NewOrderObservedEvent should have trigger source from scheduler");

            AuditRecord scheduledRecord =
                    auditRecords.stream()
                            .filter(r -> "OrderScheduledEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "NewOrderObservedEvent",
                    scheduledRecord.getEventMetadata().getTriggerSource(),
                    "OrderScheduledEvent should have trigger source from NewOrderObservedEvent");
        }

        @Test
        void shouldCaptureCorrectContextInAuditRecords() {
            String orderId = "CONTEXT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            UUID correlationId = UUID.randomUUID();

            TriggerContext triggerContext = TriggerContext.of("TestTrigger", correlationId, null);
            LocalDateTime scheduledPickupTime = LocalDateTime.now().plusHours(2);

            NewOrderObservedEvent event =
                    createTestEventWithScheduling(orderId, scheduledPickupTime, triggerContext);

            transactionTemplate.execute(
                    status -> {
                        eventPublisher.publishEvent(event);
                        return null;
                    });

            List<AuditRecord> auditRecords =
                    auditRecordRepository.findByCorrelationId(correlationId);

            assertFalse(
                    auditRecords.isEmpty(),
                    "Should have audit records for correlationId: " + correlationId);

            assertTrue(
                    auditRecords.size() >= 1,
                    "Should have at least one audit record, found: " + auditRecords.size());

            AuditRecord newOrderRecord =
                    auditRecords.stream()
                            .filter(r -> "NewOrderObservedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new AssertionError(
                                                    "NewOrderObservedEvent not found. Available"
                                                            + " events: "
                                                            + auditRecords.stream()
                                                                    .map(AuditRecord::getEventName)
                                                                    .collect(
                                                                            java.util.stream
                                                                                    .Collectors
                                                                                    .joining(
                                                                                            ", "))));

            assertEquals(
                    "Observation Context",
                    newOrderRecord.getEventMetadata().getContext(),
                    "NewOrderObservedEvent should be in Observation Context");

            auditRecords.stream()
                    .filter(r -> "OrderScheduledEvent".equals(r.getEventName()))
                    .findFirst()
                    .ifPresent(
                            scheduledRecord ->
                                    assertEquals(
                                            "Order Context",
                                            scheduledRecord.getEventMetadata().getContext(),
                                            "OrderScheduledEvent should be in Order Context"));
        }

        private NewOrderObservedEvent createTestEventWithScheduling(
                String orderId, LocalDateTime scheduledPickupTime, TriggerContext triggerContext) {
            List<ObservedOrderItem> items =
                    Arrays.asList(
                            new ObservedOrderItem(
                                    "SKU-001", "Product 1", 10, new BigDecimal("100.00")),
                            new ObservedOrderItem(
                                    "SKU-002", "Product 2", 5, new BigDecimal("50.00")));

            ObservationResult observationResult =
                    new ObservationResult(
                            orderId,
                            "John Doe",
                            "john@example.com",
                            "123 Main St",
                            "STANDARD",
                            "WH-001",
                            "NEW",
                            scheduledPickupTime,
                            items,
                            LocalDateTime.now());

            return new NewOrderObservedEvent("observer-1", observationResult, triggerContext);
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
