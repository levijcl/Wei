package com.wei.orchestrator.integration.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.application.eventhandler.PickingTaskSubmittedEventHandler;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.CommitmentStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.wes.domain.event.PickingTaskSubmittedEvent;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class PickingTaskSubmittedEventHandlerIntegrationTest {

    @Autowired private OrderRepository orderRepository;

    @Autowired private PickingTaskRepository pickingTaskRepository;

    @Autowired private PickingTaskSubmittedEventHandler eventHandler;

    @Autowired private TransactionTemplate transactionTemplate;

    @Nested
    class EventPublicationAndHandling {

        @Test
        void shouldMarkLineItemAsInProgressWhenTaskSubmitted() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00"))));
            order.markReadyForFulfillment();
            String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
            order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");
            orderRepository.save(order);

            List<TaskItem> items = List.of(TaskItem.of("SKU-001", 10, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask = pickingTaskRepository.save(pickingTask);
            pickingTask.submitToWes(WesTaskId.of("WES-TASK-001"));
            pickingTaskRepository.save(pickingTask);

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            pickingTask.getTaskId(),
                            "WES-TASK-001",
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskSubmitted(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getCommitmentInfo()
                            .getStatus());
            assertEquals(
                    pickingTask.getTaskId(),
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getCommitmentInfo()
                            .getWesTransactionId());
        }

        @Test
        void shouldMarkMultipleLineItemsAsInProgressWhenMultipleSkus() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-100", 5, new BigDecimal("50.00")),
                                    new OrderLineItem("SKU-101", 3, new BigDecimal("30.00"))));
            order.markReadyForFulfillment();
            String lineItemId1 = order.getOrderLineItems().get(0).getLineItemId();
            String lineItemId2 = order.getOrderLineItems().get(1).getLineItemId();
            order.reserveLineItem(lineItemId1, "TX-001", "EXT-RES-001", "WH-001");
            order.reserveLineItem(lineItemId2, "TX-002", "EXT-RES-002", "WH-001");
            orderRepository.save(order);

            List<TaskItem> items =
                    List.of(TaskItem.of("SKU-100", 5, "WH-001"), TaskItem.of("SKU-101", 3, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask = pickingTaskRepository.save(pickingTask);
            pickingTask.submitToWes(WesTaskId.of("WES-TASK-002"));
            pickingTaskRepository.save(pickingTask);

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            pickingTask.getTaskId(),
                            "WES-TASK-002",
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskSubmitted(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getCommitmentInfo()
                            .getStatus());
            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(1)
                            .getCommitmentInfo()
                            .getStatus());
        }

        @Test
        void shouldSkipProcessingWhenOriginIsWesDirect() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-200", 7, new BigDecimal("70.00"))));
            order.markReadyForFulfillment();
            String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
            order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");
            orderRepository.save(order);

            List<TaskItem> items = List.of(TaskItem.of("SKU-200", 7, "WH-001"));
            PickingTask pickingTask = PickingTask.createFromWesTask("WES-DIRECT-001", items, 5);
            pickingTaskRepository.save(pickingTask);

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            pickingTask.getTaskId(),
                            "WES-DIRECT-001",
                            TaskOrigin.WES_DIRECT,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskSubmitted(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertNull(foundOrder.get().getOrderLineItems().get(0).getCommitmentInfo());
        }

        @Test
        void shouldThrowExceptionWhenPickingTaskNotFound() {
            String taskId = "NON-EXISTENT-TASK";

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            taskId,
                            "WES-TASK-003",
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handlePickingTaskSubmitted(event);
                            });

            assertTrue(exception.getMessage().contains("Picking task not found"));
        }

        @Test
        void shouldSkipProcessingWhenPickingTaskHasNoOrderId() {
            List<TaskItem> items = List.of(TaskItem.of("SKU-300", 3, "WH-001"));
            PickingTask pickingTask = PickingTask.createFromWesTask("WES-TASK-004", items, 5);
            pickingTask = pickingTaskRepository.save(pickingTask);

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            pickingTask.getTaskId(),
                            "WES-TASK-004",
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskSubmitted(event);
        }

        @Test
        void shouldThrowExceptionWhenOrderNotFound() {
            String orderId = "NON-EXISTENT-ORDER";

            List<TaskItem> items = List.of(TaskItem.of("SKU-400", 2, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask = pickingTaskRepository.save(pickingTask);
            pickingTask.submitToWes(WesTaskId.of("WES-TASK-005"));
            pickingTaskRepository.save(pickingTask);

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            pickingTask.getTaskId(),
                            "WES-TASK-005",
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handlePickingTaskSubmitted(event);
                            });

            assertTrue(exception.getMessage().contains("Order not found"));
        }

        @Test
        void shouldMarkOnlyMatchingSkusAsInProgress() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-500", 1, new BigDecimal("10.00")),
                                    new OrderLineItem("SKU-501", 2, new BigDecimal("20.00"))));
            order.markReadyForFulfillment();
            String lineItemId1 = order.getOrderLineItems().get(0).getLineItemId();
            String lineItemId2 = order.getOrderLineItems().get(1).getLineItemId();
            order.reserveLineItem(lineItemId1, "TX-001", "EXT-RES-001", "WH-001");
            order.reserveLineItem(lineItemId2, "TX-002", "EXT-RES-002", "WH-001");
            orderRepository.save(order);

            List<TaskItem> items = List.of(TaskItem.of("SKU-500", 1, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask = pickingTaskRepository.save(pickingTask);
            pickingTask.submitToWes(WesTaskId.of("WES-TASK-006"));
            pickingTaskRepository.save(pickingTask);

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            pickingTask.getTaskId(),
                            "WES-TASK-006",
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskSubmitted(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getCommitmentInfo()
                            .getStatus());
            assertNull(foundOrder.get().getOrderLineItems().get(1).getCommitmentInfo());
        }
    }

    @Nested
    class TransactionIsolation {

        @Test
        void shouldCommitOrderUpdateWhenHandlerSucceeds() {
            String orderId = "SUCCESS-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        Order order =
                                new Order(
                                        orderId,
                                        List.of(
                                                new OrderLineItem(
                                                        "SKU-600", 1, new BigDecimal("10.00"))));
                        order.markReadyForFulfillment();
                        String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
                        order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");
                        orderRepository.save(order);
                        return null;
                    });

            List<TaskItem> items = List.of(TaskItem.of("SKU-600", 1, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask = pickingTaskRepository.save(pickingTask);
            pickingTask.submitToWes(WesTaskId.of("WES-TASK-007"));
            pickingTaskRepository.save(pickingTask);

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            pickingTask.getTaskId(),
                            "WES-TASK-007",
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskSubmitted(event);

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent());
            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    order.get().getOrderLineItems().get(0).getCommitmentInfo().getStatus());
        }

        @Test
        void shouldRollbackWhenHandlerFails() {
            String orderId = "FAIL-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        Order order =
                                new Order(
                                        orderId,
                                        List.of(
                                                new OrderLineItem(
                                                        "SKU-700", 2, new BigDecimal("20.00"))));
                        order.markReadyForFulfillment();
                        String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
                        order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");
                        orderRepository.save(order);
                        return null;
                    });

            String nonExistentTaskId = "NON-EXISTENT-TASK";
            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            nonExistentTaskId,
                            "WES-TASK-008",
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            assertThrows(
                    IllegalStateException.class,
                    () -> {
                        eventHandler.handlePickingTaskSubmitted(event);
                    });

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent());
            assertNull(
                    order.get().getOrderLineItems().get(0).getCommitmentInfo(),
                    "Line item should not have commitmentInfo");
        }
    }

    @Nested
    class IdempotencyScenarios {

        @Test
        void shouldBeIdempotentWhenProcessedMultipleTimes() {
            String orderId = "IDEMPOTENT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-800", 1, new BigDecimal("10.00"))));
            order.markReadyForFulfillment();
            String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
            order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");
            orderRepository.save(order);

            List<TaskItem> items = List.of(TaskItem.of("SKU-800", 1, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask = pickingTaskRepository.save(pickingTask);
            pickingTask.submitToWes(WesTaskId.of("WES-TASK-009"));
            pickingTaskRepository.save(pickingTask);

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            pickingTask.getTaskId(),
                            "WES-TASK-009",
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskSubmitted(event);
            eventHandler.handlePickingTaskSubmitted(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getCommitmentInfo()
                            .getStatus());
        }
    }
}
