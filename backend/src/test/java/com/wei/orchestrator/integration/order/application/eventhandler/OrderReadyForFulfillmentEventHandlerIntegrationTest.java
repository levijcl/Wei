package com.wei.orchestrator.integration.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.domain.model.valueobject.ExternalReservationId;
import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.command.InitiateFulfillmentCommand;
import com.wei.orchestrator.order.application.eventhandler.OrderReadyForFulfillmentEventHandler;
import com.wei.orchestrator.order.domain.event.OrderReadyForFulfillmentEvent;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.shared.domain.model.AuditRecord;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.shared.domain.repository.AuditRecordRepository;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.port.WesPort;
import java.math.BigDecimal;
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
class OrderReadyForFulfillmentEventHandlerIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;

    @Autowired private OrderRepository orderRepository;

    @Autowired private OrderReadyForFulfillmentEventHandler eventHandler;

    @Autowired private OrderApplicationService orderApplicationService;

    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private AuditRecordRepository auditRecordRepository;

    @Autowired private InventoryApplicationService inventoryApplicationService;

    @MockitoBean private InventoryPort inventoryPort;
    @MockitoBean private WesPort wesPort;

    @Nested
    class EventPublicationAndHandling {

        @Test
        void shouldHandleOrderReadyForFulfillmentEvent() {
            when(inventoryPort.createReservation(any(), any(), any(), anyInt()))
                    .thenReturn(ExternalReservationId.of("ext-reservation-123"));
            when(wesPort.submitPickingTask(any()))
                    .thenReturn(WesTaskId.of(UUID.randomUUID().toString()));

            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            orderRepository.save(order);

            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);

            eventHandler.handleOrderReadyForFulfillment(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.RESERVED, foundOrder.get().getStatus());
        }

        @Test
        void shouldReserveInventoryForOrderLineItems() {
            when(inventoryPort.createReservation(any(), any(), any(), anyInt()))
                    .thenReturn(ExternalReservationId.of("ext-reservation-123"));

            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-100", 5, new BigDecimal("50.00")),
                                    new OrderLineItem("SKU-101", 3, new BigDecimal("30.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            orderRepository.save(order);

            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);

            eventHandler.handleOrderReadyForFulfillment(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
        }

        @Test
        void shouldThrowExceptionWhenOrderNotFound() {
            String orderId = "NON-EXISTENT-ORDER";
            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleOrderReadyForFulfillment(event);
                            });

            assertTrue(exception.getMessage().contains("Order not found"));
            assertTrue(exception.getMessage().contains(orderId));
        }
    }

    @Nested
    class TransactionIsolation {

        @Test
        void shouldNotRollbackInitiateFulfillmentWhenHandlerFailsAfterCommit() {
            String orderId = "CALLER-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        List<CreateOrderCommand.OrderLineItemDto> items =
                                List.of(
                                        new CreateOrderCommand.OrderLineItemDto(
                                                "SKU-400", 1, new BigDecimal("10.00")));
                        CreateOrderCommand createCommand = new CreateOrderCommand(orderId, items);
                        orderApplicationService.createOrder(createCommand, TriggerContext.manual());
                        return null;
                    });

            when(inventoryPort.createReservation(any(), any(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Inventory system down"));

            InitiateFulfillmentCommand fulfillmentCommand = new InitiateFulfillmentCommand(orderId);

            orderApplicationService.initiateFulfillment(
                    fulfillmentCommand, TriggerContext.manual());

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(
                    order.isPresent(),
                    "Order should still exist (created in separate transaction)");
            assertEquals(OrderStatus.FAILED_TO_RESERVE, order.get().getStatus());
        }

        @Test
        void shouldCommitBothTransactionsWhenHandlerSucceeds() {
            when(inventoryPort.createReservation(any(), any(), any(), anyInt()))
                    .thenReturn(ExternalReservationId.of("ext-reservation-success"));
            when(wesPort.submitPickingTask(any()))
                    .thenReturn(WesTaskId.of(UUID.randomUUID().toString()));

            String orderId = "SUCCESS-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        List<CreateOrderCommand.OrderLineItemDto> items =
                                List.of(
                                        new CreateOrderCommand.OrderLineItemDto(
                                                "SKU-500", 2, new BigDecimal("20.00")));
                        CreateOrderCommand createCommand = new CreateOrderCommand(orderId, items);
                        orderApplicationService.createOrder(createCommand, TriggerContext.manual());
                        return null;
                    });

            InitiateFulfillmentCommand fulfillmentCommand = new InitiateFulfillmentCommand(orderId);
            orderApplicationService.initiateFulfillment(
                    fulfillmentCommand, TriggerContext.manual());

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent(), "Order should exist");
            assertEquals(
                    OrderStatus.RESERVED,
                    order.get().getStatus(),
                    "Order status should be RESERVED");
        }
    }

    @Nested
    class EventCorrelation {

        @Test
        void shouldRecordEventsWithSameCorrelationIdWhenInventoryReservedSuccessfully() {
            when(inventoryPort.createReservation(any(), any(), any(), anyInt()))
                    .thenReturn(ExternalReservationId.of("ext-reservation-123"));
            when(wesPort.submitPickingTask(any()))
                    .thenReturn(WesTaskId.of(UUID.randomUUID().toString()));

            String orderId = "CORR-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            UUID correlationId = UUID.randomUUID();

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            orderRepository.save(order);

            TriggerContext triggerContext =
                    TriggerContext.of("Scheduled:FulfillmentScheduler", correlationId, null);

            OrderReadyForFulfillmentEvent event =
                    new OrderReadyForFulfillmentEvent(orderId, triggerContext);

            transactionTemplate.execute(
                    status -> {
                        eventPublisher.publishEvent(event);
                        return null;
                    });

            List<AuditRecord> auditRecords =
                    auditRecordRepository.findByCorrelationId(correlationId);

            assertFalse(auditRecords.isEmpty(), "Should have audit records");
            assertEquals(4, auditRecords.size(), "Should have exactly 2 audit records");

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
                    eventNames.contains("OrderReadyForFulfillmentEvent"),
                    "Should audit OrderReadyForFulfillmentEvent");
            assertTrue(
                    eventNames.contains("InventoryReservedEvent"),
                    "Should audit InventoryReservedEvent");
            assertTrue(eventNames.contains("OrderReservedEvent"));
            assertTrue(eventNames.contains("PickingTaskSubmittedEvent"));

            AuditRecord orderReadyRecord =
                    auditRecords.stream()
                            .filter(r -> "OrderReadyForFulfillmentEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "Scheduled:FulfillmentScheduler",
                    orderReadyRecord.getEventMetadata().getTriggerSource(),
                    "OrderReadyForFulfillmentEvent should have trigger source from scheduler");

            AuditRecord inventoryReservedRecord =
                    auditRecords.stream()
                            .filter(r -> "InventoryReservedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "OrderReadyForFulfillmentEvent",
                    inventoryReservedRecord.getEventMetadata().getTriggerSource(),
                    "InventoryReservedEvent should have trigger source from"
                            + " OrderReadyForFulfillmentEvent");
        }

        @Test
        void shouldRecordEventsWithSameCorrelationIdWhenReservationFails() throws Exception {
            when(inventoryPort.createReservation(any(), any(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Insufficient inventory"));

            String orderId = "FAIL-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            UUID correlationId = UUID.randomUUID();

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-002", 5, new BigDecimal("50.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            orderRepository.save(order);

            TriggerContext triggerContext =
                    TriggerContext.of("Scheduled:FulfillmentScheduler", correlationId, null);

            OrderReadyForFulfillmentEvent event =
                    new OrderReadyForFulfillmentEvent(orderId, triggerContext);

            transactionTemplate.execute(
                    status -> {
                        eventPublisher.publishEvent(event);
                        return null;
                    });

            List<AuditRecord> auditRecords =
                    auditRecordRepository.findByCorrelationId(correlationId);

            assertFalse(auditRecords.isEmpty(), "Should have audit records");
            assertEquals(
                    3,
                    auditRecords.size(),
                    "Should have exactly 3 audit records (OrderReady + ReservationFailed +"
                            + " TransactionFailed)");

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
                    eventNames.contains("OrderReadyForFulfillmentEvent"),
                    "Should audit OrderReadyForFulfillmentEvent");
            assertTrue(
                    eventNames.contains("ReservationFailedEvent"),
                    "Should audit ReservationFailedEvent");
            assertTrue(
                    eventNames.contains("InventoryTransactionFailedEvent"),
                    "Should audit InventoryTransactionFailedEvent");

            AuditRecord orderReadyRecord =
                    auditRecords.stream()
                            .filter(r -> "OrderReadyForFulfillmentEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "Scheduled:FulfillmentScheduler",
                    orderReadyRecord.getEventMetadata().getTriggerSource(),
                    "OrderReadyForFulfillmentEvent should have trigger source from scheduler");

            AuditRecord reservationFailedRecord =
                    auditRecords.stream()
                            .filter(r -> "ReservationFailedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "OrderReadyForFulfillmentEvent",
                    reservationFailedRecord.getEventMetadata().getTriggerSource(),
                    "ReservationFailedEvent should have trigger source from"
                            + " OrderReadyForFulfillmentEvent");

            AuditRecord transactionFailedRecord =
                    auditRecords.stream()
                            .filter(r -> "InventoryTransactionFailedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "OrderReadyForFulfillmentEvent",
                    transactionFailedRecord.getEventMetadata().getTriggerSource(),
                    "InventoryTransactionFailedEvent should have trigger source from"
                            + " OrderReadyForFulfillmentEvent");
        }

        @Test
        void shouldCaptureCorrectContextInAuditRecords() {
            when(inventoryPort.createReservation(any(), any(), any(), anyInt()))
                    .thenReturn(ExternalReservationId.of("ext-reservation-456"));

            String orderId = "CONTEXT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            UUID correlationId = UUID.randomUUID();

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-003", 3, new BigDecimal("30.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            orderRepository.save(order);

            TriggerContext triggerContext = TriggerContext.of("TestTrigger", correlationId, null);

            OrderReadyForFulfillmentEvent event =
                    new OrderReadyForFulfillmentEvent(orderId, triggerContext);

            transactionTemplate.execute(
                    status -> {
                        eventPublisher.publishEvent(event);
                        return null;
                    });

            List<AuditRecord> auditRecords =
                    auditRecordRepository.findByCorrelationId(correlationId);

            AuditRecord orderReadyRecord =
                    auditRecords.stream()
                            .filter(r -> "OrderReadyForFulfillmentEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "Order Context",
                    orderReadyRecord.getEventMetadata().getContext(),
                    "OrderReadyForFulfillmentEvent should be in Order Context");

            auditRecords.stream()
                    .filter(r -> "InventoryReservedEvent".equals(r.getEventName()))
                    .findFirst()
                    .ifPresent(
                            inventoryReservedRecord ->
                                    assertEquals(
                                            "Inventory Context",
                                            inventoryReservedRecord.getEventMetadata().getContext(),
                                            "InventoryReservedEvent should be in Inventory"
                                                    + " Context"));
        }
    }
}
