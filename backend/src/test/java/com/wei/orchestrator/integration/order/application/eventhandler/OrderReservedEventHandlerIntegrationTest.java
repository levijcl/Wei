package com.wei.orchestrator.integration.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.wei.orchestrator.order.application.eventhandler.OrderReservedEventHandler;
import com.wei.orchestrator.order.domain.event.OrderReservedEvent;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class OrderReservedEventHandlerIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;

    @Autowired private OrderRepository orderRepository;

    @Autowired private OrderReservedEventHandler eventHandler;

    @Autowired private PickingTaskRepository pickingTaskRepository;

    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoBean private WesPort wesPort;

    @BeforeEach
    void setUp() {
        when(wesPort.submitPickingTask(any()))
                .thenReturn(WesTaskId.of(UUID.randomUUID().toString()));
    }

    @Nested
    class EventPublicationAndHandling {

        @Test
        void shouldHandleOrderReservedEvent() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            order.reserveLineItem(
                    order.getOrderLineItems().get(0).getLineItemId(),
                    "TX-001",
                    "EXT-001",
                    "WH-001");
            orderRepository.save(order);

            OrderReservedEvent event =
                    new OrderReservedEvent(
                            orderId, List.of(order.getOrderLineItems().get(0).getLineItemId()));

            eventHandler.handleOrderReserved(event);

            List<PickingTask> pickingTasks = pickingTaskRepository.findByOrderId(orderId);
            assertEquals(1, pickingTasks.size());

            PickingTask task = pickingTasks.get(0);
            assertEquals(orderId, task.getOrderId());
            assertEquals(1, task.getItems().size());
            assertEquals("SKU-001", task.getItems().get(0).getSku());
            assertEquals(10, task.getItems().get(0).getQuantity());
            assertEquals("WH-001", task.getItems().get(0).getLocation());
            assertEquals(TaskStatus.SUBMITTED, task.getStatus());
        }

        @Test
        void shouldOnlyCreateTaskWithReservedItems() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-100", 5, new BigDecimal("50.00")),
                                    new OrderLineItem("SKU-101", 3, new BigDecimal("30.00")),
                                    new OrderLineItem("SKU-102", 7, new BigDecimal("70.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            order.reserveLineItem(
                    order.getOrderLineItems().get(0).getLineItemId(),
                    "TX-001",
                    "EXT-001",
                    "WH-001");
            order.reserveLineItem(
                    order.getOrderLineItems().get(2).getLineItemId(),
                    "TX-002",
                    "EXT-002",
                    "WH-002");
            orderRepository.save(order);

            OrderReservedEvent event =
                    new OrderReservedEvent(
                            orderId,
                            List.of(
                                    order.getOrderLineItems().get(0).getLineItemId(),
                                    order.getOrderLineItems().get(2).getLineItemId()));

            eventHandler.handleOrderReserved(event);

            List<PickingTask> pickingTasks = pickingTaskRepository.findByOrderId(orderId);
            assertEquals(1, pickingTasks.size());

            PickingTask task = pickingTasks.get(0);
            assertEquals(2, task.getItems().size());
            assertTrue(task.getItems().stream().anyMatch(item -> "SKU-100".equals(item.getSku())));
            assertTrue(task.getItems().stream().anyMatch(item -> "SKU-102".equals(item.getSku())));
            assertFalse(task.getItems().stream().anyMatch(item -> "SKU-101".equals(item.getSku())));
        }

        @Test
        void shouldHandlePartialReservation() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-200", 2, new BigDecimal("20.00")),
                                    new OrderLineItem("SKU-201", 4, new BigDecimal("40.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            order.reserveLineItem(
                    order.getOrderLineItems().get(0).getLineItemId(),
                    "TX-003",
                    "EXT-003",
                    "WH-003");
            orderRepository.save(order);

            OrderReservedEvent event =
                    new OrderReservedEvent(
                            orderId, List.of(order.getOrderLineItems().get(0).getLineItemId()));

            eventHandler.handleOrderReserved(event);

            List<PickingTask> pickingTasks = pickingTaskRepository.findByOrderId(orderId);
            assertEquals(1, pickingTasks.size());

            PickingTask task = pickingTasks.get(0);
            assertEquals(1, task.getItems().size());
            assertEquals("SKU-200", task.getItems().get(0).getSku());
        }

        @Test
        void shouldThrowExceptionWhenOrderNotFound() {
            String orderId = "NON-EXISTENT-ORDER";

            OrderReservedEvent event = new OrderReservedEvent(orderId, List.of("LINE-ITEM-001"));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleOrderReserved(event);
                            });

            assertTrue(exception.getMessage().contains("Order not found"));
        }

        @Test
        void shouldLogErrorWhenPickingTaskCreationFails() {
            when(wesPort.submitPickingTask(any())).thenThrow(new RuntimeException("WES is down"));

            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-300", 1, new BigDecimal("10.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            order.reserveLineItem(
                    order.getOrderLineItems().get(0).getLineItemId(),
                    "TX-004",
                    "EXT-004",
                    "WH-004");
            orderRepository.save(order);

            OrderReservedEvent event =
                    new OrderReservedEvent(
                            orderId, List.of(order.getOrderLineItems().get(0).getLineItemId()));

            assertDoesNotThrow(() -> eventHandler.handleOrderReserved(event));

            List<PickingTask> pickingTasks = pickingTaskRepository.findByOrderId(orderId);
            assertEquals(1, pickingTasks.size());
            assertEquals(TaskStatus.FAILED, pickingTasks.get(0).getStatus());
        }

        @Test
        void shouldMapWarehouseIdCorrectly() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-400", 8, new BigDecimal("80.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            order.reserveLineItem(
                    order.getOrderLineItems().get(0).getLineItemId(),
                    "TX-005",
                    "EXT-005",
                    "WH-SPECIAL");
            orderRepository.save(order);

            OrderReservedEvent event =
                    new OrderReservedEvent(
                            orderId, List.of(order.getOrderLineItems().get(0).getLineItemId()));

            eventHandler.handleOrderReserved(event);

            List<PickingTask> pickingTasks = pickingTaskRepository.findByOrderId(orderId);
            PickingTask task = pickingTasks.get(0);

            assertEquals("WH-SPECIAL", task.getItems().get(0).getLocation());
        }
    }

    @Nested
    class TransactionIsolation {

        @Test
        void shouldCommitPickingTaskWhenHandlerSucceeds() {
            String orderId = "SUCCESS-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        Order order =
                                new Order(
                                        orderId,
                                        List.of(
                                                new OrderLineItem(
                                                        "SKU-500", 2, new BigDecimal("20.00"))));
                        order.createOrder();
                        order.markReadyForFulfillment();
                        order.reserveLineItem(
                                order.getOrderLineItems().get(0).getLineItemId(),
                                "TX-006",
                                "EXT-006",
                                "WH-006");
                        orderRepository.save(order);
                        return null;
                    });

            Optional<Order> savedOrder = orderRepository.findById(orderId);
            assertTrue(savedOrder.isPresent());

            OrderReservedEvent event =
                    new OrderReservedEvent(
                            orderId,
                            List.of(savedOrder.get().getOrderLineItems().get(0).getLineItemId()));

            eventHandler.handleOrderReserved(event);

            List<PickingTask> pickingTasks = pickingTaskRepository.findByOrderId(orderId);
            assertEquals(1, pickingTasks.size());
            assertEquals(TaskStatus.SUBMITTED, pickingTasks.get(0).getStatus());
        }

        @Test
        void shouldRollbackWhenOrderNotFound() {
            String orderId = "NON-EXISTENT-ORDER";

            OrderReservedEvent event = new OrderReservedEvent(orderId, List.of("LINE-ITEM-001"));

            assertThrows(
                    IllegalStateException.class,
                    () -> {
                        eventHandler.handleOrderReserved(event);
                    });

            List<PickingTask> pickingTasks = pickingTaskRepository.findByOrderId(orderId);
            assertTrue(pickingTasks.isEmpty());
        }
    }

    @Nested
    class MultipleItemScenarios {

        @Test
        void shouldCreateTaskWithAllReservedItems() {
            String orderId = "MULTI-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-600", 1, new BigDecimal("10.00")),
                                    new OrderLineItem("SKU-601", 2, new BigDecimal("20.00")),
                                    new OrderLineItem("SKU-602", 3, new BigDecimal("30.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            order.reserveLineItem(
                    order.getOrderLineItems().get(0).getLineItemId(),
                    "TX-007",
                    "EXT-007",
                    "WH-007");
            order.reserveLineItem(
                    order.getOrderLineItems().get(1).getLineItemId(),
                    "TX-008",
                    "EXT-008",
                    "WH-007");
            order.reserveLineItem(
                    order.getOrderLineItems().get(2).getLineItemId(),
                    "TX-009",
                    "EXT-009",
                    "WH-007");
            orderRepository.save(order);

            OrderReservedEvent event =
                    new OrderReservedEvent(
                            orderId,
                            List.of(
                                    order.getOrderLineItems().get(0).getLineItemId(),
                                    order.getOrderLineItems().get(1).getLineItemId(),
                                    order.getOrderLineItems().get(2).getLineItemId()));

            eventHandler.handleOrderReserved(event);

            List<PickingTask> pickingTasks = pickingTaskRepository.findByOrderId(orderId);
            assertEquals(1, pickingTasks.size());

            PickingTask task = pickingTasks.get(0);
            assertEquals(3, task.getItems().size());
            assertEquals(orderId, task.getOrderId());
        }
    }
}
