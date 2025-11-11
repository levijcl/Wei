package com.wei.orchestrator.integration.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.application.eventhandler.PickingTaskFailedEventHandler;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.wes.domain.event.PickingTaskFailedEvent;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
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
class PickingTaskFailedEventHandlerIntegrationTest {

    @Autowired private OrderRepository orderRepository;

    @Autowired private PickingTaskRepository pickingTaskRepository;

    @Autowired private PickingTaskFailedEventHandler eventHandler;

    @Autowired private TransactionTemplate transactionTemplate;

    @Nested
    class EventPublicationAndHandling {

        @Test
        void shouldMarkLineItemAsFailedWhenTaskFailed() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "WES timeout error";

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
            pickingTask.markFailed(reason);
            pickingTaskRepository.save(pickingTask);

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            pickingTask.getTaskId(),
                            null,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskFailed(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).hasCommitmentFailed());
            assertEquals(
                    reason,
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getCommitmentInfo()
                            .getFailureReason());
        }

        @Test
        void shouldMarkMultipleLineItemsAsFailedWhenMultipleSkus() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "Inventory unavailable";

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
                    List.of(
                            TaskItem.of("SKU-100", 5, "WH-001"),
                            TaskItem.of("SKU-101", 3, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask = pickingTaskRepository.save(pickingTask);
            pickingTask.markFailed(reason);
            pickingTaskRepository.save(pickingTask);

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            pickingTask.getTaskId(),
                            null,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskFailed(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).hasCommitmentFailed());
            assertTrue(foundOrder.get().getOrderLineItems().get(1).hasCommitmentFailed());
        }

        @Test
        void shouldSkipProcessingWhenOriginIsWesDirect() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "WES internal error";

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
            pickingTask = pickingTaskRepository.save(pickingTask);
            pickingTask.markFailed(reason);
            pickingTaskRepository.save(pickingTask);

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            pickingTask.getTaskId(),
                            "WES-DIRECT-001",
                            null,
                            TaskOrigin.WES_DIRECT,
                            reason,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskFailed(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertFalse(foundOrder.get().getOrderLineItems().get(0).hasCommitmentFailed());
        }

        @Test
        void shouldSkipProcessingWhenOrderIdIsNull() {
            String reason = "WES error";

            List<TaskItem> items = List.of(TaskItem.of("SKU-300", 3, "WH-001"));
            PickingTask pickingTask = PickingTask.createFromWesTask("WES-TASK-001", items, 5);
            pickingTask = pickingTaskRepository.save(pickingTask);

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            pickingTask.getTaskId(),
                            "WES-TASK-001",
                            null,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskFailed(event);
        }

        @Test
        void shouldThrowExceptionWhenPickingTaskNotFound() {
            String taskId = "NON-EXISTENT-TASK";
            String orderId = "ORDER-001";
            String reason = "WES error";

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            taskId,
                            null,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handlePickingTaskFailed(event);
                            });

            assertTrue(exception.getMessage().contains("Picking task not found"));
        }

        @Test
        void shouldThrowExceptionWhenOrderNotFound() {
            String orderId = "NON-EXISTENT-ORDER";
            String reason = "WES error";

            List<TaskItem> items = List.of(TaskItem.of("SKU-400", 2, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask = pickingTaskRepository.save(pickingTask);
            pickingTask.markFailed(reason);
            pickingTaskRepository.save(pickingTask);

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            pickingTask.getTaskId(),
                            null,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handlePickingTaskFailed(event);
                            });

            assertTrue(exception.getMessage().contains("Order not found"));
        }

        @Test
        void shouldMarkOnlyMatchingSkusAsFailed() {
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "Partial picking failure";

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
            pickingTask.markFailed(reason);
            pickingTaskRepository.save(pickingTask);

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            pickingTask.getTaskId(),
                            null,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskFailed(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).hasCommitmentFailed());
            assertFalse(foundOrder.get().getOrderLineItems().get(1).hasCommitmentFailed());
        }
    }

    @Nested
    class TransactionIsolation {

        @Test
        void shouldCommitOrderUpdateWhenHandlerSucceeds() {
            String orderId = "SUCCESS-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "WES timeout";

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
            pickingTask.markFailed(reason);
            pickingTaskRepository.save(pickingTask);

            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            pickingTask.getTaskId(),
                            null,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            eventHandler.handlePickingTaskFailed(event);

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent());
            assertTrue(order.get().getOrderLineItems().get(0).hasCommitmentFailed());
            assertEquals(
                    reason,
                    order.get().getOrderLineItems().get(0).getCommitmentInfo().getFailureReason());
        }

        @Test
        void shouldRollbackWhenHandlerFails() {
            String orderId = "FAIL-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            String reason = "WES error";

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
            PickingTaskFailedEvent event =
                    new PickingTaskFailedEvent(
                            nonExistentTaskId,
                            null,
                            orderId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            reason,
                            LocalDateTime.now());

            assertThrows(
                    IllegalStateException.class,
                    () -> {
                        eventHandler.handlePickingTaskFailed(event);
                    });

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent());
            assertFalse(
                    order.get().getOrderLineItems().get(0).hasCommitmentFailed(),
                    "Line item should not have failed commitment");
        }
    }
}
