package com.wei.orchestrator.unit.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.order.application.eventhandler.PickingTaskSubmittedEventHandler;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.CommitmentStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.wes.domain.event.PickingTaskSubmittedEvent;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
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
class PickingTaskSubmittedEventHandlerTest {

    @Mock private OrderRepository orderRepository;

    @Mock private PickingTaskRepository pickingTaskRepository;

    @InjectMocks private PickingTaskSubmittedEventHandler eventHandler;

    @Nested
    class handlePickingTaskSubmittedTest {

        @Test
        void shouldMarkLineItemsAsPickingInProgressWhenOrchestratorSubmitted() {
            String taskId = "PICK-001";
            String wesTaskId = "WES-001";
            String orderId = "ORDER-001";

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            taskId,
                            wesTaskId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            Order order = createOrderWithSingleItem(orderId, "SKU-001");
            PickingTask pickingTask = createPickingTask(taskId, orderId, "SKU-001");

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            eventHandler.handlePickingTaskSubmitted(event);

            verify(pickingTaskRepository, times(1)).findById(taskId);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, times(1)).save(order);

            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    order.getOrderLineItems().get(0).getCommitmentInfo().getStatus());
        }

        @Test
        void shouldMarkMultipleLineItemsWhenMultipleSkusInTask() {
            String taskId = "PICK-002";
            String wesTaskId = "WES-002";
            String orderId = "ORDER-002";

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            taskId,
                            wesTaskId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            Order order = createOrderWithMultipleItems(orderId);
            PickingTask pickingTask =
                    createPickingTaskWithMultipleSkus(taskId, orderId, "SKU-001", "SKU-002");

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            eventHandler.handlePickingTaskSubmitted(event);

            verify(orderRepository, times(1)).save(order);

            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    order.getOrderLineItems().get(0).getCommitmentInfo().getStatus());
            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    order.getOrderLineItems().get(1).getCommitmentInfo().getStatus());
        }

        @Test
        void shouldSkipProcessingWhenOriginIsWesDirect() {
            String taskId = "PICK-003";
            String wesTaskId = "WES-003";

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            taskId, wesTaskId, TaskOrigin.WES_DIRECT, LocalDateTime.now());

            eventHandler.handlePickingTaskSubmitted(event);

            verify(pickingTaskRepository, never()).findById(any());
            verify(orderRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenPickingTaskNotFound() {
            String taskId = "PICK-004";
            String wesTaskId = "WES-004";

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            taskId,
                            wesTaskId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.empty());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handlePickingTaskSubmitted(event);
                            });

            assertTrue(exception.getMessage().contains("Picking task not found"));
            assertTrue(exception.getMessage().contains(taskId));
            verify(pickingTaskRepository, times(1)).findById(taskId);
            verify(orderRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldSkipProcessingWhenPickingTaskHasNoOrderId() {
            String taskId = "PICK-005";
            String wesTaskId = "WES-005";

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            taskId,
                            wesTaskId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            PickingTask pickingTask = createPickingTaskWithNoOrderId(taskId);

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));

            eventHandler.handlePickingTaskSubmitted(event);

            verify(pickingTaskRepository, times(1)).findById(taskId);
            verify(orderRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldThrowExceptionWhenOrderNotFound() {
            String taskId = "PICK-006";
            String wesTaskId = "WES-006";
            String orderId = "ORDER-006";

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            taskId,
                            wesTaskId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            PickingTask pickingTask = createPickingTask(taskId, orderId, "SKU-001");

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handlePickingTaskSubmitted(event);
                            });

            assertTrue(exception.getMessage().contains("Order not found"));
            assertTrue(exception.getMessage().contains(orderId));
            verify(pickingTaskRepository, times(1)).findById(taskId);
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, never()).save(any());
        }

        @Test
        void shouldMarkOnlyMatchingSkusAsInProgress() {
            String taskId = "PICK-007";
            String wesTaskId = "WES-007";
            String orderId = "ORDER-007";

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            taskId,
                            wesTaskId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            Order order = createOrderWithMultipleItems(orderId);
            PickingTask pickingTask = createPickingTask(taskId, orderId, "SKU-001");

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            eventHandler.handlePickingTaskSubmitted(event);

            verify(orderRepository, times(1)).save(order);

            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    order.getOrderLineItems().get(0).getCommitmentInfo().getStatus());
            assertNull(order.getOrderLineItems().get(1).getCommitmentInfo());
        }

        @Test
        void shouldBeIdempotentWhenCalledMultipleTimes() {
            String taskId = "PICK-008";
            String wesTaskId = "WES-008";
            String orderId = "ORDER-008";

            PickingTaskSubmittedEvent event =
                    new PickingTaskSubmittedEvent(
                            taskId,
                            wesTaskId,
                            TaskOrigin.ORCHESTRATOR_SUBMITTED,
                            LocalDateTime.now());

            Order order = createOrderWithSingleItem(orderId, "SKU-001");
            PickingTask pickingTask = createPickingTask(taskId, orderId, "SKU-001");

            when(pickingTaskRepository.findById(taskId)).thenReturn(Optional.of(pickingTask));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            eventHandler.handlePickingTaskSubmitted(event);
            eventHandler.handlePickingTaskSubmitted(event);

            verify(orderRepository, times(2)).save(order);

            assertEquals(
                    CommitmentStatus.IN_PROGRESS,
                    order.getOrderLineItems().get(0).getCommitmentInfo().getStatus());
        }
    }

    private Order createOrderWithSingleItem(String orderId, String sku) {
        List<OrderLineItem> lineItems =
                List.of(new OrderLineItem(sku, 10, new BigDecimal("100.00")));
        Order order = new Order(orderId, lineItems);
        order.markReadyForFulfillment();
        String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
        order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");
        return order;
    }

    private Order createOrderWithMultipleItems(String orderId) {
        List<OrderLineItem> lineItems =
                List.of(
                        new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")),
                        new OrderLineItem("SKU-002", 5, new BigDecimal("50.00")));
        Order order = new Order(orderId, lineItems);
        order.markReadyForFulfillment();
        String lineItemId1 = order.getOrderLineItems().get(0).getLineItemId();
        String lineItemId2 = order.getOrderLineItems().get(1).getLineItemId();
        order.reserveLineItem(lineItemId1, "TX-001", "EXT-RES-001", "WH-001");
        order.reserveLineItem(lineItemId2, "TX-002", "EXT-RES-002", "WH-001");
        return order;
    }

    private PickingTask createPickingTask(String taskId, String orderId, String sku) {
        List<TaskItem> items = List.of(TaskItem.of(sku, 10, "WH-001"));
        PickingTask task = PickingTask.createForOrder(orderId, items, 5);
        task.setTaskId(taskId);
        return task;
    }

    private PickingTask createPickingTaskWithMultipleSkus(
            String taskId, String orderId, String... skus) {
        List<TaskItem> items = new java.util.ArrayList<>();
        for (String sku : skus) {
            items.add(TaskItem.of(sku, 10, "WH-001"));
        }
        PickingTask task = PickingTask.createForOrder(orderId, items, 5);
        task.setTaskId(taskId);
        return task;
    }

    private PickingTask createPickingTaskWithNoOrderId(String taskId) {
        List<TaskItem> items = List.of(TaskItem.of("SKU-001", 10, "WH-001"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-001", items, 5);
        task.setTaskId(taskId);
        return task;
    }
}
